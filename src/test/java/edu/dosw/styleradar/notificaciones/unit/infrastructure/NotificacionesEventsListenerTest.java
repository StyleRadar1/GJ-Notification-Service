package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionDesactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionReactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaRetiradaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp.NotificacionesEventsListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class NotificacionesEventsListenerTest {

    private ProcesarEventosExternosUseCase useCase;
    private NotificacionesEventsListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(ProcesarEventosExternosUseCase.class);
        listener = new NotificacionesEventsListener(useCase);
    }

    @Test
    @DisplayName("NOT-06 - Una reposicion (stock 0 a mayor que 0) se procesa con los datos del evento")
    void reposicionSeProcesa() {
        listener.onInventarioActualizado(inventario(0, 8, 2));

        ArgumentCaptor<DatosPrendaVO> captor = ArgumentCaptor.forClass(DatosPrendaVO.class);
        verify(useCase).procesarDisponibilidadPrenda(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo("1045");
        assertThat(captor.getValue().stockDisponible()).isEqualTo(8);
        assertThat(captor.getValue().cicloReposicion()).isEqualTo(2);
    }

    @Test
    @DisplayName("NOT-06 - Un evento sobre una prenda que ya tenia stock no es reposicion y se ignora")
    void prendaConStockPrevioSeIgnora() {
        listener.onInventarioActualizado(inventario(5, 8, 2));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-06 - Un evento que sigue en cero no es reposicion y se ignora")
    void stockQueSigueEnCeroSeIgnora() {
        listener.onInventarioActualizado(inventario(0, 0, 2));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-06 / RES-133 - Un evento sin stock anterior se procesa como reposicion si stockActual > 0")
    void eventoSinStockAnteriorSeProcesaSiStockActualEsMayorACero() {
        listener.onInventarioActualizado(inventario(null, 8, 2));

        ArgumentCaptor<DatosPrendaVO> captor = ArgumentCaptor.forClass(DatosPrendaVO.class);
        verify(useCase).procesarDisponibilidadPrenda(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo("1045");
        assertThat(captor.getValue().stockDisponible()).isEqualTo(8);
    }

    @Test
    @DisplayName("NOT-06 - Un evento sin stock actual es invalido y va directo a la cola de errores")
    void eventoSinStockActualEsInvalido() {
        assertThatThrownBy(() -> listener.onInventarioActualizado(inventario(0, null, 2)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-06 - Un evento con itemId UUID se procesa correctamente")
    void eventoConItemIdUuidSeProcesaCorrectamente() {
        String uuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        InventarioActualizadoEventPayload payload = new InventarioActualizadoEventPayload(
                "evt-inv-uuid", uuid, "Camisa Lino", "CAMISAS", "BLANCO", "L", "BogotaStreet", "SASTRE",
                new BigDecimal("120000"), 0, 4, 1, "15", "USAQUEN", LocalDateTime.now());

        listener.onInventarioActualizado(payload);

        ArgumentCaptor<DatosPrendaVO> captor = ArgumentCaptor.forClass(DatosPrendaVO.class);
        verify(useCase).procesarDisponibilidadPrenda(captor.capture());
        assertThat(captor.getValue().itemId()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("NOT-09 - Un estado de cuenta desconocido es invalido y va directo a la cola de errores")
    void estadoDeCuentaDesconocidoEsInvalido() {
        UsuarioEstadoCambiadoEventPayload payload = new UsuarioEstadoCambiadoEventPayload(
                "evt-usr-1", 501L, "mateo@styleradar.co", "DESCONOCIDO", LocalDateTime.now());

        assertThatThrownBy(() -> listener.onUsuarioEstadoCambiado(payload))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-09 - Una cuenta SUSPENDIDA se guarda con ese estado y deja de recibir avisos")
    void cuentaSuspendidaSeGuarda() {
        listener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload(
                "evt-usr-2", 501L, "mateo@styleradar.co", "SUSPENDIDO", LocalDateTime.now()));

        verify(useCase).actualizarEstadoCuenta(501L, "mateo@styleradar.co",
                edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.SUSPENDIDO);
        org.assertj.core.api.Assertions.assertThat(new edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia(
                501L, "mateo@styleradar.co",
                edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.SUSPENDIDO, LocalDateTime.now())
                .puedeRecibirNotificaciones()).isFalse();
    }

    @Test
    @DisplayName("NOT-08 - Una prenda retirada desactiva las alertas del item")
    void prendaRetiradaDesactivaLasAlertasDelItem() {
        listener.onPrendaRetirada(new PrendaRetiradaEventPayload("evt-cat-1", "55", "Retirada por la tienda", LocalDateTime.now()));

        verify(useCase).desactivarAlertasPorItemEliminado("55");
    }

    @Test
    @DisplayName("NOT-08 - Un evento de prenda retirada sin item es invalido y va directo a la cola de errores")
    void prendaRetiradaSinItemEsInvalida() {
        assertThatThrownBy(() -> listener.onPrendaRetirada(new PrendaRetiradaEventPayload("evt-cat-2", (String) null, null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onPrendaRetirada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("GJ-117 - Una fundacion desactivada avisa a cada donante afectado, con su motivo y la clave del evento")
    void fundacionDesactivadaAvisaACadaDonante() {
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "evt-fun-1", 7L, "Fundacion Sol", List.of(101L, 102L), "cerro sus operaciones", LocalDateTime.now()));

        verify(useCase).generarNotificacionDirecta(eq(101L), eq(TipoNotificacion.CAMBIO_DONACION),
                eq("Fundacion desactivada"),
                eq("La fundacion Fundacion Sol ha sido desactivada: cerro sus operaciones"),
                eq("/donaciones"), eq(null), eq("evt-fun-1"));
        verify(useCase).generarNotificacionDirecta(eq(102L), eq(TipoNotificacion.CAMBIO_DONACION),
                eq("Fundacion desactivada"), any(), eq("/donaciones"), eq(null), eq("evt-fun-1"));
    }

    @Test
    @DisplayName("GJ-117 - Sin motivo ni nombre el mensaje queda completo y sin dos puntos sobrantes")
    void fundacionDesactivadaSinMotivoNiNombre() {
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "evt-fun-2", 7L, null, List.of(101L), " ", null));

        verify(useCase).generarNotificacionDirecta(eq(101L), eq(TipoNotificacion.CAMBIO_DONACION),
                eq("Fundacion desactivada"), eq("La fundacion una fundacion aliada ha sido desactivada"),
                eq("/donaciones"), eq(null), eq("evt-fun-2"));
    }

    @Test
    @DisplayName("GJ-117 - Sin donantes afectados no hay a quien avisar y el evento termina sin error")
    void fundacionDesactivadaSinDonantesTerminaLimpio() {
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload("evt-fun-3", 7L, "X", null, null, null));
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload("evt-fun-4", 7L, "X", List.of(), null, null));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("GJ-117 - Un donante nulo dentro de la lista se omite")
    void fundacionDesactivadaOmiteDonantesNulos() {
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "evt-fun-5", 7L, "X", Arrays.asList(null, 105L), null, null));

        verify(useCase, times(1)).generarNotificacionDirecta(any(), any(), any(), any(), any(), any(), any());
        verify(useCase).generarNotificacionDirecta(eq(105L), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CADCHE v2 - Estados ACTIVA, INACTIVA, BLOQUEADA y SUSPENDIDA se normalizan correctamente")
    void estadosEnFemeninoSeNormalizanCorrectamente() {
        listener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload("e1", 12L, null, "ACTIVA", null));
        listener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload("e2", 12L, null, "INACTIVA", null));
        listener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload("e3", 12L, null, "BLOQUEADA", null));
        listener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload("e4", 12L, null, "SUSPENDIDA", null));

        verify(useCase).actualizarEstadoCuenta(12L, null, edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.ACTIVO);
        verify(useCase).actualizarEstadoCuenta(12L, null, edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.INACTIVO);
        verify(useCase).actualizarEstadoCuenta(12L, null, edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.BLOQUEADO);
        verify(useCase).actualizarEstadoCuenta(12L, null, edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta.SUSPENDIDO);
    }

    @Test
    @DisplayName("CADCHE v2 - Fundacion desactivada notifica al representante y deduplica si coincide con donante")
    void fundacionDesactivadaAvisaRepresentanteYSinDuplicar() {
        listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "evt-cad-1", 3L, "Fundación Abrigo Solidario", List.of(101L, 24L),
                "Cierre temporal", LocalDateTime.now(), 24L));

        verify(useCase, times(1)).generarNotificacionDirecta(
                eq(101L), eq(TipoNotificacion.CAMBIO_DONACION), eq("Fundacion desactivada"),
                eq("La fundacion Fundación Abrigo Solidario ha sido desactivada: Cierre temporal"),
                eq("/donaciones"), eq(null), eq("evt-cad-1"));
        verify(useCase, times(1)).generarNotificacionDirecta(
                eq(24L), eq(TipoNotificacion.CAMBIO_DONACION), eq("Fundacion desactivada"),
                eq("La fundacion Fundación Abrigo Solidario ha sido desactivada: Cierre temporal"),
                eq("/donaciones"), eq(null), eq("evt-cad-1"));
        verify(useCase, times(2)).generarNotificacionDirecta(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CADCHE v2 - Fundacion reactivada avisa al representante con su motivo o sin el")
    void fundacionReactivadaAvisaAlRepresentante() {
        listener.onFundacionReactivada(new FundacionReactivadaEventPayload(
                "evt-react-1", 3L, "Fundación Abrigo Solidario", 24L,
                "Reapertura de recepción de donaciones", LocalDateTime.now()));
        listener.onFundacionReactivada(new FundacionReactivadaEventPayload(
                "evt-react-2", 3L, null, 24L, " ", null));

        verify(useCase).generarNotificacionDirecta(
                eq(24L), eq(TipoNotificacion.CAMBIO_DONACION), eq("Fundacion reactivada"),
                eq("La fundacion Fundación Abrigo Solidario ha sido reactivada y vuelve a recibir donaciones: Reapertura de recepción de donaciones"),
                eq("/donaciones"), eq(null), eq("evt-react-1"));
        verify(useCase).generarNotificacionDirecta(
                eq(24L), eq(TipoNotificacion.CAMBIO_DONACION), eq("Fundacion reactivada"),
                eq("La fundacion una fundacion aliada ha sido reactivada y vuelve a recibir donaciones"),
                eq("/donaciones"), eq(null), eq("evt-react-2"));
    }

    @Test
    @DisplayName("CADCHE v2 - Fundacion reactivada sin representante termina limpio y si es invalida va a DLQ")
    void fundacionReactivadaCasosBordeYDlq() {
        listener.onFundacionReactivada(new FundacionReactivadaEventPayload(
                "evt-react-3", 3L, "X", null, null, null));
        verifyNoInteractions(useCase);

        assertThatThrownBy(() -> listener.onFundacionReactivada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onFundacionReactivada(new FundacionReactivadaEventPayload(
                "evt-react-4", null, "X", 24L, null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onFundacionReactivada(new FundacionReactivadaEventPayload(
                " ", 3L, "X", 24L, null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("GJ-117 - Sin fundacionId, sin eventoId o sin payload el evento va a la cola de errores")
    void fundacionDesactivadaInvalidaVaALaDlq() {
        assertThatThrownBy(() -> listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "evt-fun-6", null, "X", List.of(1L), null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                null, 7L, "X", List.of(1L), null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onFundacionDesactivada(new FundacionDesactivadaEventPayload(
                "  ", 7L, "X", List.of(1L), null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onFundacionDesactivada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(useCase, never()).generarNotificacionDirecta(any(), any(), any(), any(), any(), any(), any());
    }

    private InventarioActualizadoEventPayload inventario(Integer anterior, Integer actual, Integer ciclo) {
        return new InventarioActualizadoEventPayload(
                "evt-inv-1", "1045", "Chaqueta Bomber", "CHAQUETAS", "NEGRO", "M", "BogotaStreet", "STREETWEAR",
                new BigDecimal("185000"), anterior, actual, ciclo, "12", "CHAPINERO", LocalDateTime.now());
    }
}
