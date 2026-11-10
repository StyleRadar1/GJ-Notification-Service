package edu.dosw.styleradar.notificaciones.integration.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * GJ-59 - La notificacion generalizada: la alerta es opcional y todos los tipos se guardan y se leen
 * sin tocar las notificaciones que ya existen.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GJ-59 - Notificacion generalizada en el modelo y en la base de datos")
class NotificacionGeneralizadaPersistenciaTest {

    private static final long USUARIO_ID = 59_001L;

    @Autowired
    private NotificacionRepositoryPort notificacionRepositoryPort;

    @Test
    @DisplayName("Cada tipo de notificacion se guarda sin alerta, con enlace y como no leida, y se lee igual")
    void todosLosTiposSeGuardanSinAlertaYSeLeenIgual() {
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            Notificacion guardada = notificacionRepositoryPort.guardar(nueva(tipo, null, null, null));

            Notificacion leida = notificacionRepositoryPort.buscarPorId(guardada.getId()).orElseThrow();

            assertThat(leida.getTipo()).isEqualTo(tipo);
            assertThat(leida.getAlertaId()).isNull();
            assertThat(leida.getEnlace()).isEqualTo("/destino/" + tipo.name().toLowerCase());
            assertThat(leida.isLeida()).isFalse();
            assertThat(leida.getFechaLectura()).isNull();
        }
    }

    @Test
    @DisplayName("Existen los tipos nuevos de pago, prenda vendida y solicitud de donacion")
    void existenLosTiposNuevos() {
        assertThat(Arrays.stream(TipoNotificacion.values()).map(Enum::name))
                .contains("PAGO_CONFIRMADO", "PRENDA_VENDIDA", "SOLICITUD_DONACION");
    }

    @Test
    @DisplayName("Una notificacion de alerta existente sigue funcionando con su restriccion unica")
    void laNotificacionDeAlertaSigueFuncionandoConSuRestriccionUnica() {
        Notificacion guardada = notificacionRepositoryPort.guardar(
                nueva(TipoNotificacion.REPOSICION_STOCK, 590_001L, 590_002L, 1));

        assertThat(guardada.getId()).isNotNull();
        assertThat(notificacionRepositoryPort.existePorAlertaItemYCiclo(590_001L, 590_002L, 1)).isTrue();
        assertThat(notificacionRepositoryPort.existePorAlertaItemYCiclo(590_001L, 590_002L, 2)).isFalse();
    }

    private Notificacion nueva(TipoNotificacion tipo, Long alertaId, Long itemId, Integer ciclo) {
        return new Notificacion(null, USUARIO_ID, alertaId, itemId, ciclo, tipo, CanalNotificacion.IN_APP,
                "Titulo " + tipo, "Mensaje " + tipo, "/destino/" + tipo.name().toLowerCase(), false,
                LocalDateTime.now(), null);
    }
}
