package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SR-RNF-15: las pruebas normales usan H2 en modo PostgreSQL; esta corre las migraciones de Flyway y la
 * deduplicacion contra un PostgreSQL 16 real. Requiere Docker; sin Docker se omite (en el CI si corre).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Persistencia contra PostgreSQL real")
class PostgresRealIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Test
    @DisplayName("Flyway aplica todas las migraciones y Hibernate valida el esquema")
    void flywayAplicaLasMigraciones() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL", Integer.class);
        assertThat(aplicadas).isGreaterThanOrEqualTo(6);

        for (String tabla : new String[] {"alertas", "notificaciones", "preferencias_avisos", "correos_pendientes"}) {
            Integer existe = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, tabla);
            assertThat(existe).as("tabla " + tabla).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("La restriccion unica (evento_id, usuario_id) impide avisar dos veces por el mismo evento")
    void restriccionUnicaPorEventoYUsuario() {
        String sql = "INSERT INTO notificaciones (usuario_id, tipo, canal, titulo, mensaje, evento_id)"
                + " VALUES (?, 'MENSAJE_NUEVO', 'IN_APP', 'Titulo', 'Mensaje', ?)";
        jdbc.update(sql, 9_801L, "evt-pg-unico");

        assertThatThrownBy(() -> jdbc.update(sql, 9_801L, "evt-pg-unico"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(sql, 9_802L, "evt-pg-unico");

        Integer filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notificaciones WHERE evento_id = 'evt-pg-unico'", Integer.class);
        assertThat(filas).isEqualTo(2);
    }

    @Test
    @DisplayName("Guardar dos veces la misma notificacion de reposicion deja una sola fila")
    void unaSolaNotificacionPorAlertaPrendaYCiclo() {
        Notificacion primera = notificacionDeReposicion();
        Notificacion segunda = notificacionDeReposicion();

        assertThat(notificaciones.guardar(primera).getId()).isNotNull();
        assertThat(notificaciones.guardar(segunda)).isNotNull();

        Integer filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notificaciones WHERE alerta_id = 9810 AND ciclo_reposicion = 3", Integer.class);
        assertThat(filas).isEqualTo(1);
        assertThat(notificaciones.existePorAlertaItemYCiclo(9_810L, "pg-item-1", 3)).isTrue();
    }

    private Notificacion notificacionDeReposicion() {
        return new Notificacion(null, 9_811L, 9_810L, "pg-item-1", 3, TipoNotificacion.REPOSICION_STOCK,
                CanalNotificacion.IN_APP, "Prenda disponible", "Ya hay unidades", "/catalogo/prendas/pg-item-1",
                false, LocalDateTime.now(), null);
    }
}
