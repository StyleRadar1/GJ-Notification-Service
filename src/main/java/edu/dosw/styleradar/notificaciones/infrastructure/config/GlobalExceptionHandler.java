package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ConflictoException;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errores.merge(e.getField(), String.valueOf(e.getDefaultMessage()), (a, b) -> a + "; " + b));
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Error de validacion en la solicitud", detail, request, errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleParameterValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getAllValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(resultado -> {
            String parametro = resultado.getMethodParameter().getParameterName();
            String mensaje = resultado.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            errores.merge(parametro != null ? parametro : "parametro", mensaje, (a, b) -> a + "; " + b);
        });
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Error de validacion en la solicitud", detail, request, errores);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Parametro invalido",
                "El valor del parametro '" + ex.getName() + "' no es valido",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String mensaje = "El parametro '" + ex.getParameterName() + "' es obligatorio";
        Map<String, String> errores = new LinkedHashMap<>();
        errores.put(ex.getParameterName(), mensaje);
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Error de validacion en la solicitud", mensaje, request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Cuerpo de solicitud malformado",
                "El formato JSON enviado no es valido",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage(), request);
    }

    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            RecursoNoEncontradoException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                "La ruta solicitada no existe",
                request
        );
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            ConflictoException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(HttpStatus.CONFLICT, "Conflicto de estado del recurso", ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(
            ReglaDeNegocioException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Violacion de regla de negocio",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ServicioExternoNoDisponibleException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceUnavailable(
            ServicioExternoNoDisponibleException ex,
            HttpServletRequest request
    ) {
        return buildProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio externo no disponible",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Error interno no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                "Ocurrio un error inesperado al procesar la solicitud",
                request
        );
    }

    private ResponseEntity<ProblemDetail> buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        return buildProblemDetail(status, title, detail, request, null);
    }

    /** {@code errores} lleva un mensaje por campo en los 400 de validacion; en los demas errores va vacio. */
    private ResponseEntity<ProblemDetail> buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request,
            Map<String, String> errores
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("mensaje", detail);
        if (errores != null && !errores.isEmpty()) {
            problem.setProperty("errors", errores);
        }
        return ResponseEntity.status(status).body(problem);
    }
}
