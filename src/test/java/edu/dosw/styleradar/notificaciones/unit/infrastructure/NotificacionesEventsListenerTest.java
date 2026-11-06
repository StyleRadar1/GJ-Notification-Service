package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaRetiradaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp.NotificacionesEventsListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        assertThat(captor.getValue().itemId()).isEqualTo(1045L);
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
    @DisplayName("NOT-06 - Un evento sin stock anterior es invalido y va directo a la cola de errores")
    void eventoSinStockAnteriorEsInvalido() {
        assertThatThrownBy(() -> listener.onInventarioActualizado(inventario(null, 8, 2)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-09 - Un estado de cuenta desconocido es invalido y va directo a la cola de errores")
    void estadoDeCuentaDesconocidoEsInvalido() {
        UsuarioEstadoCambiadoEventPayload payload = new UsuarioEstadoCambiadoEventPayload(
                "evt-usr-1", 501L, "mateo@styleradar.co", "SUSPENDIDO", LocalDateTime.now());

        assertThatThrownBy(() -> listener.onUsuarioEstadoCambiado(payload))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("NOT-08 - Una prenda retirada desactiva las alertas del item")
    void prendaRetiradaDesactivaLasAlertasDelItem() {
        listener.onPrendaRetirada(new PrendaRetiradaEventPayload("evt-cat-1", 55L, "Retirada por la tienda", LocalDateTime.now()));

        verify(useCase).desactivarAlertasPorItemEliminado(55L);
    }

    @Test
    @DisplayName("NOT-08 - Un evento de prenda retirada sin item es invalido y va directo a la cola de errores")
    void prendaRetiradaSinItemEsInvalida() {
        assertThatThrownBy(() -> listener.onPrendaRetirada(new PrendaRetiradaEventPayload("evt-cat-2", null, null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onPrendaRetirada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }

    private InventarioActualizadoEventPayload inventario(Integer anterior, Integer actual, Integer ciclo) {
        return new InventarioActualizadoEventPayload(
                "evt-inv-1", 1045L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", "M", "BogotaStreet", "STREETWEAR",
                new BigDecimal("185000"), anterior, actual, ciclo, 12L, "CHAPINERO", LocalDateTime.now());
    }
}
