package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.infrastructure.security.AuthenticatedUserPrincipal;
import edu.dosw.styleradar.notificaciones.infrastructure.security.ProblemDetailSecurityHandlers;
import edu.dosw.styleradar.notificaciones.infrastructure.security.RateLimitFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("Limite de solicitudes por usuario (A06)")
class RateLimitFilterTest {

    private final AtomicLong reloj = new AtomicLong(1_000_000L);
    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final ProblemDetailSecurityHandlers manejadores = new ProblemDetailSecurityHandlers(new ObjectMapper());

    private RateLimitFilter filtro(boolean habilitado, int maximo) {
        return new RateLimitFilter(manejadores, registro, habilitado, maximo);
    }

    private RateLimitFilter filtroConReloj(int maximo) throws Exception {
        var constructor = RateLimitFilter.class.getDeclaredConstructor(
                ProblemDetailSecurityHandlers.class, io.micrometer.core.instrument.MeterRegistry.class,
                boolean.class, int.class, java.util.function.LongSupplier.class);
        constructor.setAccessible(true);
        return constructor.newInstance(manejadores, registro, true, maximo, (java.util.function.LongSupplier) reloj::get);
    }

    @BeforeEach
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(long usuarioId) {
        var principal = new AuthenticatedUserPrincipal(usuarioId, "u@styleradar.co", "COMPRADOR", List.of("COMPRADOR"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private MockHttpServletResponse llamar(RateLimitFilter filtro, String metodo, String uri) throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest(metodo, uri);
        solicitud.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(solicitud, respuesta, new MockFilterChain());
        return respuesta;
    }

    @Test
    @DisplayName("Deja pasar hasta el maximo y rechaza la siguiente con 429, Retry-After y ProblemDetail")
    void rechazaAlSuperarElMaximo() throws Exception {
        RateLimitFilter filtro = filtroConReloj(3);
        autenticar(1L);
        for (int i = 0; i < 3; i++) {
            assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        }

        MockHttpServletResponse rechazada = llamar(filtro, "GET", "/api/v1/users/101/alerts");

        assertThat(rechazada.getStatus()).isEqualTo(429);
        assertThat(rechazada.getHeader("Retry-After")).isNotNull();
        assertThat(rechazada.getContentType()).contains("application/problem+json");
        assertThat(rechazada.getContentAsString()).contains("Demasiadas solicitudes").contains("3 solicitudes por minuto");
        assertThat(registro.get("notificaciones.ratelimit.rechazadas").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("El cupo es por usuario: otro usuario no se ve afectado")
    void cadaUsuarioTieneSuCupo() throws Exception {
        RateLimitFilter filtro = filtroConReloj(1);
        autenticar(1L);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(429);

        autenticar(2L);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Al pasar el minuto el cupo se reinicia")
    void laVentanaSeReinicia() throws Exception {
        RateLimitFilter filtro = filtroConReloj(1);
        autenticar(1L);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(429);

        reloj.addAndGet(60_001L);

        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sin autenticar se cuenta por IP, tomando la primera de X-Forwarded-For")
    void sinAutenticarSeCuentaPorIp() throws Exception {
        RateLimitFilter filtro = filtroConReloj(1);
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/v1/users/101/alerts");
            solicitud.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.9");
            MockHttpServletResponse respuesta = new MockHttpServletResponse();
            filtro.doFilter(solicitud, respuesta, new MockFilterChain());
            assertThat(respuesta.getStatus()).isEqualTo(i == 0 ? 200 : 429);
        }
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Health, Swagger, OPTIONS y el filtro apagado no cuentan")
    void exclusiones() throws Exception {
        RateLimitFilter filtro = filtroConReloj(1);
        for (int i = 0; i < 5; i++) {
            assertThat(llamar(filtro, "GET", "/actuator/health").getStatus()).isEqualTo(200);
            assertThat(llamar(filtro, "GET", "/swagger-ui/index.html").getStatus()).isEqualTo(200);
            assertThat(llamar(filtro, "GET", "/v3/api-docs").getStatus()).isEqualTo(200);
            assertThat(llamar(filtro, "OPTIONS", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
        }
        RateLimitFilter apagado = filtro(false, 1);
        for (int i = 0; i < 5; i++) {
            assertThat(llamar(apagado, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Con la configuracion publica (constructor de Spring) tambien limita")
    void constructorPublico() throws Exception {
        RateLimitFilter filtro = filtro(true, 1);
        autenticar(5L);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(200);
        assertThat(llamar(filtro, "GET", "/api/v1/users/101/alerts").getStatus()).isEqualTo(429);
    }
}
