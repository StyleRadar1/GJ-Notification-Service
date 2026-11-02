package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.security;

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
                return 1L;
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
}
