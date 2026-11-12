package edu.dosw.styleradar.notificaciones.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Respuestas 401 y 403 del filtro de seguridad con el mismo cuerpo ProblemDetail que el resto de errores,
 * para que el frontend siempre tenga un mensaje que mostrar.
 */
@Component
public class ProblemDetailSecurityHandlers {

    private final ObjectMapper objectMapper;

    public ProblemDetailSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> escribir(
                request, response, HttpStatus.UNAUTHORIZED, "No autenticado", "Token JWT ausente, invalido o expirado");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> escribir(
                request, response, HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permisos para realizar esta accion");
    }

    private void escribir(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus estado,
            String titulo,
            String detalle
    ) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://styleradar.co/errors/" + estado.value()));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("timestamp", Instant.now().toString());
        problema.setProperty("mensaje", detalle);
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
