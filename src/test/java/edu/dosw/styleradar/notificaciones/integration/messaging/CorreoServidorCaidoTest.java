package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** GJ-64 caso negativo - Con el servidor de correo caido, el aviso en plataforma se crea igual. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3999",
        "spring.mail.properties.mail.smtp.connectiontimeout=500",
        "spring.mail.properties.mail.smtp.timeout=500",
        "application.mail.retry.initial-interval-ms=10"
})
@DisplayName("GJ-64 - Servidor de correo caido")
class CorreoServidorCaidoTest {

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventos;

    @Autowired
    private EstadoCuentaCopiaRepositoryPort cuentas;

    @Autowired
    private PreferenciaAvisoRepositoryPort preferencias;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Test
    @DisplayName("Si el servidor de correo falla, la notificacion en plataforma se crea igual y no hay excepcion")
    void laNotificacionSeCreaAunqueFalleElCorreo() {
        long usuario = 6_411L;
        cuentas.guardar(new EstadoCuentaCopia(usuario, "u6411@styleradar.co", EstadoCuenta.ACTIVO, LocalDateTime.now()));
        preferencias.guardar(new PreferenciaAviso(usuario, TipoNotificacion.MENSAJE_NUEVO, CanalAviso.CORREO_Y_PLATAFORMA, null));

        Notificacion[] creada = new Notificacion[1];
        assertThatCode(() -> creada[0] = procesarEventos.generarNotificacionDirecta(
                usuario, TipoNotificacion.MENSAJE_NUEVO, "Aviso con SMTP caido", "Texto", "/chat/9", null))
                .doesNotThrowAnyException();

        assertThat(creada[0]).isNotNull();
        assertThat(notificaciones.listarPorUsuario(usuario, null)).hasSize(1);
    }
}
