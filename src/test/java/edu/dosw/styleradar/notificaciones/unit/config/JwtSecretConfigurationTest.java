package edu.dosw.styleradar.notificaciones.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class JwtSecretConfigurationTest {

    private static final String SECRET_KEY = "application.security.jwt.secret-key";

    @Test
    void perfilSinDefaultExigeLaVariableJwtSecret() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(cargar("application.yml"));
        environment.getPropertySources().addLast(cargar("application-prod.yml"));

        assertThatThrownBy(() -> environment.getRequiredProperty(SECRET_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void perfilSinDefaultUsaElSecretoProvistoPorEntorno() throws IOException {
        MockEnvironment environment = new MockEnvironment().withProperty("JWT_SECRET", "secreto-de-entorno");
        environment.getPropertySources().addLast(cargar("application.yml"));

        assertThat(environment.getRequiredProperty(SECRET_KEY)).isEqualTo("secreto-de-entorno");
    }

    @Test
    void perfilDeDesarrolloTieneValorLocalParaArrancarConUnSoloComando() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(cargar("application-dev.yml"));
        environment.getPropertySources().addLast(cargar("application.yml"));

        assertThat(environment.getRequiredProperty(SECRET_KEY)).startsWith("solo_desarrollo_local");
    }

    private PropertySource<?> cargar(String recurso) throws IOException {
        return new YamlPropertySourceLoader().load(recurso, new ClassPathResource(recurso)).get(0);
    }
}
