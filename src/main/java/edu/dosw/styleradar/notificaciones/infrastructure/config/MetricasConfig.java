package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.sse.SseEmitterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metricas propias del servicio (OWASP A09, registro y alertas). Se leen en /actuator/metrics y
 * /actuator/prometheus, que exigen rol de administrador; las reglas de alerta estan en
 * docs/observabilidad/alertas-prometheus.yml.
 */
@Slf4j
@Configuration
public class MetricasConfig {

    private static final int TOPE_DE_CORREOS_CONTADOS = 1_000;

    @Bean
    MeterBinder conexionesSse(SseEmitterRegistry registro) {
        return meterRegistry -> Gauge.builder("notificaciones.sse.conexiones.activas", registro, SseEmitterRegistry::contarSuscripcionesTotales)
                .description("Conexiones SSE abiertas (campanas conectadas) en esta instancia")
                .register(meterRegistry);
    }

    @Bean
    MeterBinder correosPendientes(CorreoPendienteRepositoryPort puerto) {
        return meterRegistry -> Gauge.builder("notificaciones.correos.pendientes", puerto, this::contarPendientes)
                .description("Correos sin enviar que esperan un reintento (cuenta hasta " + TOPE_DE_CORREOS_CONTADOS + ")")
                .register(meterRegistry);
    }

    private double contarPendientes(CorreoPendienteRepositoryPort puerto) {
        try {
            return puerto.listarPendientes(TOPE_DE_CORREOS_CONTADOS).size();
        } catch (RuntimeException ex) {
            log.debug("No se pudo contar los correos pendientes para la metrica: {}", ex.getMessage());
            return Double.NaN;
        }
    }
}
