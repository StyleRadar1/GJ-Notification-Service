package edu.dosw.styleradar.notificaciones.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Pool de conexiones a PostgreSQL")
class PoolConexionesConfigTest {

    @Test
    @DisplayName("application.yml fija un pool chico para el PostgreSQL compartido")
    void poolChico() throws Exception {
        List<PropertySource<?>> fuentes =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        PropertySource<?> fuente = fuentes.get(0);

        assertThat(fuente.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("${DB_POOL_MAX:3}");
        assertThat(fuente.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("${DB_POOL_MIN_IDLE:1}");
        assertThat(fuente.getProperty("spring.datasource.hikari.idle-timeout"))
                .isEqualTo("${DB_POOL_IDLE_TIMEOUT_MS:30000}");
    }

    @Test
    @DisplayName("Los perfiles dev y prod no sobrescriben el pool")
    void perfilesNoSobrescriben() throws Exception {
        for (String archivo : List.of("application-dev.yml", "application-prod.yml")) {
            List<PropertySource<?>> fuentes =
                    new YamlPropertySourceLoader().load(archivo, new ClassPathResource(archivo));
            assertThat(fuentes.get(0).getProperty("spring.datasource.hikari.maximum-pool-size")).isNull();
        }
    }
}
