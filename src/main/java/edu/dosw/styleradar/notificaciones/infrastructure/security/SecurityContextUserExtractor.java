package edu.dosw.styleradar.notificaciones.infrastructure.security;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUserExtractor {

    public Long obtenerUsuarioAutenticadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccesoDenegadoException("Usuario no autenticado");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal customPrincipal) {
            return customPrincipal.usuarioId();
        }
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException ex) {
                throw new AccesoDenegadoException("Identificador de usuario invalido en el contexto de seguridad");
            }
        }
        if (principal instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new AccesoDenegadoException("Identificador de usuario invalido en el contexto de seguridad");
            }
        }
        throw new AccesoDenegadoException("No fue posible determinar el usuario autenticado");
    }

    public AuthenticatedUserPrincipal obtenerPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        throw new AccesoDenegadoException("No fue posible determinar el usuario autenticado");
    }

    public Long validarAccesoUsuario(String usuarioId) {
        Long usuarioAutenticadoId = obtenerUsuarioAutenticadoId();
        if (usuarioId == null || usuarioId.isBlank() || "me".equalsIgnoreCase(usuarioId)) {
            return usuarioAutenticadoId;
        }
        Long idSolicitado;
        try {
            idSolicitado = Long.parseLong(usuarioId.trim());
        } catch (NumberFormatException ex) {
            throw new AccesoDenegadoException("Identificador de usuario invalido en la ruta");
        }
        if (!usuarioAutenticadoId.equals(idSolicitado) && !esAdmin()) {
            throw new AccesoDenegadoException("No tienes autorizacion para acceder o modificar los recursos de otro usuario");
        }
        return idSolicitado;
    }

    public boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                            || a.getAuthority().equalsIgnoreCase("ROLE_ADMIN_STYLERADAR")
                            || a.getAuthority().equalsIgnoreCase("ROLE_STYLERADAR_ADMIN")
                            || a.getAuthority().equalsIgnoreCase("STYLERADAR_ADMIN")
                            || a.getAuthority().equalsIgnoreCase("ADMIN"));
    }
}
