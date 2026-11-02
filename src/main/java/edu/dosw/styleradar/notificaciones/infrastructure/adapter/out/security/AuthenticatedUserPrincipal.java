package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.security;

public record AuthenticatedUserPrincipal(
        Long usuarioId,
        String email,
        String role
) {
}
