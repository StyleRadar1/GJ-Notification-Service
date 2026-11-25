package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.infrastructure.security.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void propagaCorrelationIdEntranteYLimpiaMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-not-77");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteFiltro = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcDuranteFiltro.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);

        assertThat(mdcDuranteFiltro.get()).isEqualTo("corr-not-77");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("corr-not-77");
        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo("corr-not-77");
    }

    @Test
    void generaUuidSeguroCuandoFaltaOCorrompeCabecera() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "bad\r\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String generado = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generado).isNotBlank().doesNotContain("\r").doesNotContain("\n");
        assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo(generado);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
