package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/** Guarda las decisiones de seguridad de PROD (A02 y A04) para que nadie las cambie sin darse cuenta. */
@DisplayName("Configuracion de PROD")
class ConfiguracionProduccionTest {

    private Properties cargar(String archivo) {
        YamlPropertiesFactoryBean fabrica = new YamlPropertiesFactoryBean();
        fabrica.setResources(new ClassPathResource(archivo));
        return fabrica.getObject();
    }

    @Test
    @DisplayName("Swagger y /v3/api-docs vienen apagados por defecto en PROD")
    void swaggerApagadoPorDefecto() {
        Properties prod = cargar("application-prod.yml");

        assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("${SWAGGER_ENABLED:false}");
        assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo("${SWAGGER_ENABLED:false}");
    }

    @Test
    @DisplayName("El TLS hacia RabbitMQ viene encendido por defecto en PROD")
    void tlsEncendidoPorDefecto() {
        Properties prod = cargar("application-prod.yml");

        assertThat(prod.getProperty("spring.rabbitmq.ssl.enabled")).isEqualTo("${RABBITMQ_SSL_ENABLED:true}");
    }

    @Test
    @DisplayName("PROD no trae secretos por defecto: el JWT y la base de datos salen del entorno")
    void sinSecretosPorDefecto() {
        Properties base = cargar("application.yml");
        Properties prod = cargar("application-prod.yml");

        assertThat(base.getProperty("application.security.jwt.secret-key")).isEqualTo("${JWT_SECRET}");
        assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(prod.getProperty("spring.rabbitmq.password")).isEqualTo("${RABBITMQ_PASSWORD}");
    }

    @Test
    @DisplayName("Prometheus esta expuesto y el limite por minuto viene encendido")
    void prometheusYLimiteEncendidos() {
        Properties base = cargar("application.yml");

        assertThat(base.getProperty("management.endpoints.web.exposure.include")).contains("prometheus");
        assertThat(base.getProperty("application.rate-limit.enabled")).isEqualTo("${RATE_LIMIT_ENABLED:true}");
        assertThat(base.getProperty("application.security.sse-ticket.ttl-seconds")).isEqualTo("${SSE_TICKET_TTL_SECONDS:60}");
    }
}
