package edu.dosw.styleradar.notificaciones.infrastructure.security;

public record AuthenticatedUserPrincipal(
        Long usuarioId,
        String email,
        String role
) {
}
