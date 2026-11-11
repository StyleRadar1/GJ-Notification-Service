package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.application.dto.event.DonacionEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.MensajeEnviadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PagoConfirmadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaVendidaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.SolicitudDonacionCreadaEventPayload;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp.NotificacionesEventsListener;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * GJ-61 y GJ-65 - Los consumidores de eventos externos generan la notificacion correcta para el usuario
 * correcto, y un evento repetido no genera una segunda notificacion.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GJ-61 / GJ-65 - Consumidores de eventos externos")
class ConsumidoresEventosExternosTest {

    @Autowired
    private NotificacionesEventsListener listener;

    @Autowired
    private NotificacionRepositoryPort notificaciones;

    @Autowired
    private EstadoCuentaCopiaRepositoryPort cuentas;

    @Test
    @DisplayName("Un mensaje nuevo repetido (mismo eventoId) genera una sola notificacion")
    void mensajeRepetidoNoDuplica() {
        long destinatario = 6_501L;
        MensajeEnviadoEventPayload evento = new MensajeEnviadoEventPayload(
                "evt-msg-65-1", 11L, 99L, 7L, "Ana", destinatario, "Hola", LocalDateTime.now());

        listener.onMensajeEnviado(evento);
        listener.onMensajeEnviado(evento);

        assertThat(deTipo(destinatario, TipoNotificacion.MENSAJE_NUEVO)).hasSize(1);
    }

    @Test
    @DisplayName("Dos eventos distintos para el mismo usuario generan dos notificaciones")
    void eventosDistintosSiGeneran() {
        long destinatario = 6_502L;

        listener.onMensajeEnviado(new MensajeEnviadoEventPayload(
                "evt-msg-65-2a", 12L, 99L, 7L, "Ana", destinatario, "Uno", LocalDateTime.now()));
        listener.onMensajeEnviado(new MensajeEnviadoEventPayload(
                "evt-msg-65-2b", 12L, 99L, 7L, "Ana", destinatario, "Dos", LocalDateTime.now()));

        assertThat(deTipo(destinatario, TipoNotificacion.MENSAJE_NUEVO)).hasSize(2);
    }

    @Test
    @DisplayName("Una solicitud de donacion notifica a la fundacion destinataria y no se duplica")
    void solicitudDonacionNotificaALaFundacion() {
        long fundacion = 6_503L;
        SolicitudDonacionCreadaEventPayload evento = new SolicitudDonacionCreadaEventPayload(
                "evt-don-65-1", 40L, fundacion, "Carlos", "5 camisas", LocalDateTime.now());

        listener.onSolicitudDonacionCreada(evento);
        listener.onSolicitudDonacionCreada(evento);

        List<Notificacion> resultado = deTipo(fundacion, TipoNotificacion.SOLICITUD_DONACION);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMensaje()).contains("Carlos").contains("5 camisas");
        assertThat(resultado.get(0).getEnlace()).isEqualTo("/donaciones/40");
    }

    @Test
    @DisplayName("Una solicitud de donacion sin fundacion va a la cola de errores")
    void solicitudDonacionIncompletaEsInvalida() {
        assertThatThrownBy(() -> listener.onSolicitudDonacionCreada(
                new SolicitudDonacionCreadaEventPayload("evt-don-65-2", 41L, null, "Carlos", null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onSolicitudDonacionCreada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("Una prenda vendida notifica a los interesados, menos al comprador")
    void prendaVendidaNotificaALosInteresados() {
        long interesadoA = 6_504L;
        long interesadoB = 6_505L;
        long comprador = 6_506L;

        listener.onPrendaVendida(new PrendaVendidaEventPayload(
                "evt-vta-65-1", 70L, "Chaqueta bomber", comprador,
                List.of(interesadoA, interesadoB, comprador), LocalDateTime.now()));

        assertThat(deTipo(interesadoA, TipoNotificacion.PRENDA_VENDIDA)).hasSize(1);
        assertThat(deTipo(interesadoB, TipoNotificacion.PRENDA_VENDIDA)).hasSize(1);
        assertThat(deTipo(comprador, TipoNotificacion.PRENDA_VENDIDA)).isEmpty();
    }

    @Test
    @DisplayName("Una prenda vendida repetida no duplica las notificaciones")
    void prendaVendidaRepetidaNoDuplica() {
        long interesado = 6_507L;
        PrendaVendidaEventPayload evento = new PrendaVendidaEventPayload(
                "evt-vta-65-2", 71L, "Jean", 1L, List.of(interesado), LocalDateTime.now());

        listener.onPrendaVendida(evento);
        listener.onPrendaVendida(evento);

        assertThat(deTipo(interesado, TipoNotificacion.PRENDA_VENDIDA)).hasSize(1);
    }

    @Test
    @DisplayName("Una prenda vendida sin interesados no genera nada y sin publicacion es invalida")
    void prendaVendidaSinInteresadosOInvalida() {
        listener.onPrendaVendida(new PrendaVendidaEventPayload("evt-vta-65-3", 72L, "Jean", 1L, null, null));

        assertThatThrownBy(() -> listener.onPrendaVendida(
                new PrendaVendidaEventPayload("evt-vta-65-4", null, "Jean", 1L, List.of(1L), null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("Un pago confirmado notifica al usuario con su plan y no se duplica")
    void pagoConfirmadoNotificaAlUsuario() {
        long usuario = 6_508L;
        cuentas.guardar(new EstadoCuentaCopia(usuario, "u6508@styleradar.co", EstadoCuenta.ACTIVO, LocalDateTime.now()));
        PagoConfirmadoEventPayload evento = new PagoConfirmadoEventPayload(
                "evt-pago-65-1", 900L, usuario, "Radar+", LocalDateTime.now());

        listener.onPagoConfirmado(evento);
        listener.onPagoConfirmado(evento);

        List<Notificacion> resultado = deTipo(usuario, TipoNotificacion.PAGO_CONFIRMADO);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMensaje()).contains("Radar+");
    }

    @Test
    @DisplayName("Un pago de un usuario que no existe en la copia va a la cola de errores")
    void pagoDeUsuarioDesconocidoVaALaDlq() {
        assertThatThrownBy(() -> listener.onPagoConfirmado(
                new PagoConfirmadoEventPayload("evt-pago-65-2", 901L, 6_599L, "Radar+", null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onPagoConfirmado(
                new PagoConfirmadoEventPayload("evt-pago-65-3", 902L, null, "Radar+", null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("El mensaje de la donacion cambia segun el estado: aprobada, rechazada, cita u otro")
    void mensajeDeDonacionSegunEstado() {
        long donante = 6_509L;
        String[][] casos = {
                {"APROBADA", "fue aprobada"},
                {"RECHAZADA", "fue rechazada"},
                {"CITA_PROGRAMADA", "cita"},
                {"EN_REVISION", "cambio a estado EN_REVISION"},
        };
        for (int i = 0; i < casos.length; i++) {
            listener.onDonacionEstadoCambiado(new DonacionEstadoCambiadoEventPayload(
                    "evt-dona-65-" + i, 50L + i, donante, "Fundacion Sol", casos[i][0], null, LocalDateTime.now()));
        }

        List<Notificacion> resultado = deTipo(donante, TipoNotificacion.CAMBIO_DONACION);
        assertThat(resultado).hasSize(4);
        for (String[] caso : casos) {
            assertThat(resultado).anyMatch(n -> n.getMensaje().contains(caso[1]));
        }
    }

    private List<Notificacion> deTipo(long usuarioId, TipoNotificacion tipo) {
        return notificaciones.listarPorUsuario(usuarioId, null).stream()
                .filter(n -> n.getTipo() == tipo)
                .toList();
    }
}
