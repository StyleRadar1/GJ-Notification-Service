package edu.dosw.styleradar.notificaciones.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final Key signingKey;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${application.security.jwt.secret-key}") String secretKey,
            @Value("${application.security.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        byte[] keyBytes = resolveKeyBytes(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    private static byte[] resolveKeyBytes(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("JWT secret key must not be null or blank");
        }
        String trimmed = secretKey.trim();

        // 1. Standard Base64 for 256-bit key (ms-auth): exactly 44 chars with valid Base64 alphabet [A-Za-z0-9+/=]
        if (trimmed.length() == 44 && trimmed.matches("^[A-Za-z0-9+/]+=*$")) {
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed);
                if (decoded != null && decoded.length == 32) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // Not valid standard Base64
            }
        }

        // 2. Fallback for plain-text keys in local/test configurations
        byte[] utf8Bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(utf8Bytes, 0, padded, 0, utf8Bytes.length);
            return padded;
        }
        return utf8Bytes;
    }

    public String generateToken(Long usuarioId, String email, String role) {
        return generateToken(usuarioId, email, role != null ? List.of(role) : List.of("COMPRADOR"));
    }

    public String generateToken(Long usuarioId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        List<String> normalizedRoles = (roles == null || roles.isEmpty())
                ? List.of("COMPRADOR")
                : roles.stream().map(JwtTokenService::normalizeRole).toList();
        String primaryRole = normalizedRoles.get(0);

        return Jwts.builder()
                .setSubject(String.valueOf(usuarioId))
                .claim("userId", usuarioId)
                .claim("email", email != null ? email : "")
                .claim("role", primaryRole)
                .claim("roles", normalizedRoles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public Long extractUsuarioId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        String subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
            try {
                return Long.parseLong(subject.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        Object email = claims.get("email");
        if (email != null && !email.toString().isBlank()) {
            return email.toString();
        }
        return claims.getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof Collection<?> col) {
            List<String> list = col.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .map(JwtTokenService::normalizeRole)
                    .filter(r -> !r.isBlank())
                    .toList();
            if (!list.isEmpty()) {
                return list;
            }
        }
        Object singleRole = claims.get("role");
        if (singleRole != null) {
            String norm = normalizeRole(singleRole.toString());
            if (!norm.isBlank()) {
                return List.of(norm);
            }
        }
        return List.of("COMPRADOR");
    }

    public String extractRole(String token) {
        List<String> roles = extractRoles(token);
        for (String r : roles) {
            if ("ADMIN_STYLERADAR".equalsIgnoreCase(r) || "ADMIN".equalsIgnoreCase(r)) {
                return r;
            }
        }
        return roles.isEmpty() ? "COMPRADOR" : roles.get(0);
    }

    public static String normalizeRole(String rawRole) {
        if (rawRole == null) {
            return "";
        }
        String roleStr = rawRole.trim().toUpperCase();
        return roleStr.startsWith("ROLE_") ? roleStr.substring(5) : roleStr;
    }
}
