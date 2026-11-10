package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoFeignClient;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadJwtHttpTest {

    private static final String ALERTAS = "/api/v1/usuarios/me/alertas";
    private static final String NOTIFICACIONES = "/api/v1/usuarios/me/notificaciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @MockBean
    private CatalogoFeignClient catalogoFeignClient;

    @Value("${application.security.jwt.secret-key}")
    private String secretoDelServicio;

    @Test
    @DisplayName("NOT-22 - Sin token responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get(ALERTAS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(NOTIFICACIONES)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NOT-22 - Un token vencido responde 401")
    void tokenVencidoResponde401() throws Exception {
        String vencido = firmar(secretoDelServicio, 601L, new Date(System.currentTimeMillis() - 60_000));

        mockMvc.perform(get(ALERTAS).header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NOT-22 - Un token firmado con otra clave responde 401")
    void tokenConOtraFirmaResponde401() throws Exception {
        String ajeno = firmar("otra_clave_distinta_de_al_menos_treinta_y_dos_bytes_1234", 601L,
                new Date(System.currentTimeMillis() + 60_000));

        mockMvc.perform(get(ALERTAS).header("Authorization", "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NOT-22 - Un token manipulado o sin formato responde 401")
    void tokenManipuladoResponde401() throws Exception {
        String valido = jwtTokenService.generateToken(601L, "usuario@styleradar.co", "COMPRADOR");
        String manipulado = valido.substring(0, valido.length() - 4) + "abcd";

        mockMvc.perform(get(ALERTAS).header("Authorization", "Bearer " + manipulado))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ALERTAS).header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NOT-22 y NOT-05 - Otro usuario no puede desactivar mi alerta ni leer mi notificacion (403)")
    void otroUsuarioNoAccedeAMisRecursos() throws Exception {
        String propietario = jwtTokenService.generateToken(602L, "propietario@styleradar.co", "COMPRADOR");
        String intruso = jwtTokenService.generateToken(603L, "intruso@styleradar.co", "COMPRADOR");
        when(catalogoFeignClient.consultarDisponibilidad(6001L))
                .thenReturn(new CatalogoDisponibilidadItemResponse(6001L, 0, List.of()));

        String alertaJson = mockMvc.perform(post(ALERTAS)
                        .header("Authorization", "Bearer " + propietario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":6001,\"canal\":\"IN_APP\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long alertaId = objectMapper.readTree(alertaJson).get("id").asLong();

        mockMvc.perform(delete(ALERTAS + "/" + alertaId).header("Authorization", "Bearer " + intruso))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ALERTAS).header("Authorization", "Bearer " + intruso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        procesarEventosExternosUseCase.generarNotificacionDirecta(
                602L, TipoNotificacion.MENSAJE_NUEVO, "Mensaje", "Hola", "/chat/conversaciones/1", null);
        String notificacionesJson = mockMvc.perform(get(NOTIFICACIONES).header("Authorization", "Bearer " + propietario))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long notificacionId = objectMapper.readTree(notificacionesJson).get(0).get("id").asLong();

        mockMvc.perform(patch(NOTIFICACIONES + "/" + notificacionId + "/leer")
                        .header("Authorization", "Bearer " + intruso))
                .andExpect(status().isForbidden());
    }

    private String firmar(String secreto, long usuarioId, Date vencimiento) {
        return Jwts.builder()
                .setSubject(String.valueOf(usuarioId))
                .claim("userId", usuarioId)
                .claim("role", "COMPRADOR")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(vencimiento)
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
