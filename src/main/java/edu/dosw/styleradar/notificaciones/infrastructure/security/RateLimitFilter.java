package edu.dosw.styleradar.notificaciones.infrastructure.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limite de solicitudes por usuario (OWASP A06, diseno inseguro): ventana fija de un minuto. Cada usuario
 * autenticado tiene su cupo; lo que llega sin autenticar se cuenta por IP. Al pasarse responde 429 con
 * ProblemDetail y {@code Retry-After}. Se configura con RATE_LIMIT_PER_MINUTE y se apaga con
 * RATE_LIMIT_ENABLED=false. El cupo vive en memoria de cada instancia.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long VENTANA_MS = 60_000L;
    private static final int MAXIMO_DE_CLAVES = 10_000;

    private final ProblemDetailSecurityHandlers manejadores;
    private final boolean habilitado;
    private final int maximoPorMinuto;
    private final LongSupplier reloj;
    private final Counter rechazadas;
    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(
            ProblemDetailSecurityHandlers manejadores,
            MeterRegistry registro,
            @Value("${application.rate-limit.enabled:true}") boolean habilitado,
            @Value("${application.rate-limit.requests-per-minute:120}") int maximoPorMinuto
    ) {
        this(manejadores, registro, habilitado, maximoPorMinuto, System::currentTimeMillis);
    }

    RateLimitFilter(
            ProblemDetailSecurityHandlers manejadores,
            MeterRegistry registro,
            boolean habilitado,
            int maximoPorMinuto,
            LongSupplier reloj
    ) {
        this.manejadores = manejadores;
        this.habilitado = habilitado;
        this.maximoPorMinuto = maximoPorMinuto;
        this.reloj = reloj;
        this.rechazadas = Counter.builder("notificaciones.ratelimit.rechazadas")
                .description("Solicitudes rechazadas con 429 por superar el limite por minuto")
                .register(registro);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !habilitado
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/actuator/health")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long ahora = reloj.getAsLong();
        String clave = claveDe(request);
        Ventana ventana = ventanas.compute(clave, (k, actual) ->
                actual == null || ahora - actual.inicio >= VENTANA_MS ? new Ventana(ahora, 1) : actual.sumar());
        if (ventanas.size() > MAXIMO_DE_CLAVES) {
            ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicio >= VENTANA_MS);
        }

        if (ventana.cuenta > maximoPorMinuto) {
            rechazadas.increment();
            long segundos = Math.max(1, (VENTANA_MS - (ahora - ventana.inicio) + 999) / 1000);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
            manejadores.responder(request, response, HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes",
                    "Superaste el limite de " + maximoPorMinuto + " solicitudes por minuto. Intenta de nuevo en "
                            + segundos + " segundos");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String claveDe(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return "u:" + principal.usuarioId();
        }
        String reenviado = request.getHeader("X-Forwarded-For");
        String ip = reenviado != null && !reenviado.isBlank()
                ? reenviado.split(",")[0].trim()
                : request.getRemoteAddr();
        return "ip:" + ip;
    }

    private record Ventana(long inicio, int cuenta) {
        Ventana sumar() {
            return new Ventana(inicio, cuenta + 1);
        }
    }
}
