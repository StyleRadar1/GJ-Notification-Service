package edu.dosw.styleradar.notificaciones.infrastructure.security;

import org.slf4j.MDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.exception.CodigoError;
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
                request, response, HttpStatus.UNAUTHORIZED, CodigoError.UNAUTHENTICATED, "No autenticado", "Token JWT ausente, invalido o expirado");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> escribir(
                request, response, HttpStatus.FORBIDDEN, CodigoError.ACCESS_DENIED, "Acceso denegado", "No tiene permisos para realizar esta accion");
    }

    public void responder(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus estado,
            CodigoError codigo,
            String titulo,
            String detalle
    ) throws IOException {
        escribir(request, response, estado, codigo, titulo, detalle);
    }

    private void escribir(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus estado,
            CodigoError codigo,
            String titulo,
            String detalle
    ) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("timestamp", Instant.now().toString());
        problema.setProperty("code", codigo.name());
        problema.setProperty("mensaje", detalle);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problema.setProperty("correlationId", correlationId);
        }
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
