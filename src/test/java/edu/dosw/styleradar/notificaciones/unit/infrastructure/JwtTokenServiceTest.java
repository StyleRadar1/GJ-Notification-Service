package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final byte[] RAW_256_BIT_KEY = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(RAW_256_BIT_KEY);

    @Test
    @DisplayName("Debe inicializarse con un secreto codificado en Base64 de 256 bits (estandar ms-auth)")
    void inicializaConSecretoBase64() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        String token = service.generateToken(101L, "usuario@styleradar.co", List.of("COMPRADOR"));
        assertThat(service.isTokenValid(token)).isTrue();
        assertThat(service.extractUsuarioId(token)).isEqualTo(101L);
        assertThat(service.extractRole(token)).isEqualTo("COMPRADOR");
    }

    @Test
    @DisplayName("Debe soportar claves en texto plano usadas en CI y desarrollo sin decodificarlas incorrectamente como Base64")
    void soportaClaveTextoPlanoLocal() {
        String clavePlana = "solo_desarrollo_local_no_usar_en_produccion_styleradar_2026";
        JwtTokenService service = new JwtTokenService(clavePlana, 3600000L);

        // Simulamos un token firmado directamente con los bytes UTF-8 de la clave plana (como en CI / Postman)
        String tokenPlano = Jwts.builder()
                .setSubject("101")
                .claim("userId", 101L)
                .claim("role", "COMPRADOR")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(clavePlana.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.isTokenValid(tokenPlano)).isTrue();
        assertThat(service.extractUsuarioId(tokenPlano)).isEqualTo(101L);
    }

    @Test
    @DisplayName("Debe validar tokens emitidos externamente por ms-auth con sub=id y roles=lista")
    void validaTokenFormatoMsAuth() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        // Simulamos un token firmado por ms-auth con sub = "456" y roles = ["ADMIN_ALMACEN", "COMPRADOR"]
        String tokenMsAuth = Jwts.builder()
                .setSubject("456")
                .claim("roles", List.of("ADMIN_ALMACEN", "COMPRADOR"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.isTokenValid(tokenMsAuth)).isTrue();
        assertThat(service.extractUsuarioId(tokenMsAuth)).isEqualTo(456L);
        assertThat(service.extractRoles(tokenMsAuth)).containsExactly("ADMIN_ALMACEN", "COMPRADOR");
        assertThat(service.extractRole(tokenMsAuth)).isEqualTo("ADMIN_ALMACEN");
    }

    @Test
    @DisplayName("Debe priorizar ADMIN_STYLERADAR si esta presente en la lista de roles")
    void priorizaAdminStyleRadarEnListaDeRoles() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        String token = Jwts.builder()
                .setSubject("888")
                .claim("roles", List.of("COMPRADOR", "ADMIN_STYLERADAR"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.extractRoles(token)).containsExactly("COMPRADOR", "ADMIN_STYLERADAR");
        assertThat(service.extractRole(token)).isEqualTo("ADMIN_STYLERADAR");
    }

    @Test
    @DisplayName("Debe reconocer STYLERADAR_ADMIN y validar tokens CADCHE v2 (sub numerico, roles en ingles, sin email)")
    void validaFormatoCadcheV2YStyleradarAdmin() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        String tokenBuyer = Jwts.builder()
                .setId("jti-123")
                .setSubject("12")
                .claim("roles", List.of("BUYER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.isTokenValid(tokenBuyer)).isTrue();
        assertThat(service.extractUsuarioId(tokenBuyer)).isEqualTo(12L);
        assertThat(service.extractRoles(tokenBuyer)).containsExactly("BUYER");
        assertThat(service.extractRole(tokenBuyer)).isEqualTo("BUYER");
        assertThat(service.extractEmail(tokenBuyer)).isEqualTo("12");

        String tokenAdmin = Jwts.builder()
                .setSubject("99")
                .claim("roles", List.of("BUYER", "STYLERADAR_ADMIN"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.extractRole(tokenAdmin)).isEqualTo("STYLERADAR_ADMIN");
    }

    @Test
    @DisplayName("Debe manejar roles con prefijo ROLE_ y normalizarlos")
    void normalizaPrefijoRole() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        String token = Jwts.builder()
                .setSubject("999")
                .claim("roles", List.of("ROLE_REPRESENTANTE_FUNDACION"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.extractRoles(token)).containsExactly("REPRESENTANTE_FUNDACION");
        assertThat(service.extractRole(token)).isEqualTo("REPRESENTANTE_FUNDACION");
    }

    @Test
    @DisplayName("Debe rechazar token vencido o con firma invalida")
    void rechazaTokenInvalido() {
        JwtTokenService service = new JwtTokenService(BASE64_SECRET, 3600000L);

        String vencido = Jwts.builder()
                .setSubject("101")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(RAW_256_BIT_KEY), SignatureAlgorithm.HS256)
                .compact();

        assertThat(service.isTokenValid(vencido)).isFalse();
        assertThat(service.isTokenValid("token-invalido-o-malformado")).isFalse();
    }

    @Test
    @DisplayName("Debe lanzar excepcion si la clave secreta es nula o vacia")
    void fallaConClaveNulaOVacia() {
        assertThatThrownBy(() -> new JwtTokenService(null, 3600000L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtTokenService("   ", 3600000L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
