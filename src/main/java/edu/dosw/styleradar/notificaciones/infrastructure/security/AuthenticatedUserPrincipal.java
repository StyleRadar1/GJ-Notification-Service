package edu.dosw.styleradar.notificaciones.infrastructure.security;

import java.util.List;

public record AuthenticatedUserPrincipal(
        Long usuarioId,
        String email,
        String role,
        List<String> roles
) {
    public AuthenticatedUserPrincipal(Long usuarioId, String email, String role) {
        this(usuarioId, email, role, role != null ? List.of(role) : List.of());
    }
}
