package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp;

import edu.dosw.styleradar.notificaciones.application.dto.event.BusquedaGuardadaSincronizadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.DonacionEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.MensajeEnviadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.ModeracionPrendaCambiadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaPublicadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidores de eventos externos.
 *
 * <p>Un payload invalido se rechaza de inmediato hacia la cola de errores. Cualquier otra excepcion
 * se deja propagar para que la politica de reintentos ({@code RabbitRetryConfig}) reintente y, solo
 * al agotarse, envie el mensaje a la cola de errores.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionesEventsListener {

    private final ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @RabbitListener(queues = "${application.messaging.queues.inventory-updated:styleradar.notificaciones.inventario-actualizado.q}")
    public void onInventarioActualizado(InventarioActualizadoEventPayload payload) {
        if (payload == null || payload.itemId() == null) {
            throw payloadInvalido("InventarioActualizado", "sin itemId");
        }
        if (payload.stockAnterior() == null || payload.stockActual() == null) {
            throw payloadInvalido("InventarioActualizado", "sin stockAnterior o stockActual");
        }
        if (!payload.esReposicion()) {
            log.debug("InventarioActualizado del item {} no es una reposicion (stock {} a {}), se ignora",
                    payload.itemId(), payload.stockAnterior(), payload.stockActual());
            return;
        }
        DatosPrendaVO datosPrenda = new DatosPrendaVO(
                payload.itemId(),
                payload.nombrePrenda(),
                payload.categoria(),
                payload.color(),
                payload.talla() != null ? List.of(payload.talla()) : List.of(),
                payload.marca(),
                payload.estilo(),
                payload.precio(),
                payload.stockActual(),
                payload.cicloReposicion() != null ? payload.cicloReposicion() : 1,
                payload.tiendaId(),
                payload.localidad()
        );
        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(datosPrenda);
    }

    @RabbitListener(queues = "${application.messaging.queues.garment-published:styleradar.notificaciones.prenda-publicada.q}")
    public void onPrendaPublicada(PrendaPublicadaEventPayload payload) {
        if (payload == null || payload.itemId() == null) {
            throw payloadInvalido("PrendaPublicada", "sin itemId");
        }
        DatosPrendaVO datosPrenda = new DatosPrendaVO(
                payload.itemId(),
                payload.nombrePrenda(),
                payload.categoria(),
                payload.color(),
                payload.tallasDisponibles() != null ? payload.tallasDisponibles() : List.of(),
                payload.marca(),
                payload.estilo(),
                payload.precio(),
                payload.stockTotal(),
                1,
                payload.tiendaId(),
                payload.localidad()
        );
        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(datosPrenda);
    }

    @RabbitListener(queues = "${application.messaging.queues.user-status-changed:styleradar.notificaciones.usuario-estado.q}")
    public void onUsuarioEstadoCambiado(UsuarioEstadoCambiadoEventPayload payload) {
        if (payload == null || payload.usuarioId() == null || payload.nuevoEstado() == null) {
            throw payloadInvalido("UsuarioEstadoCambiado", "incompleto");
        }
        EstadoCuenta nuevoEstado;
        try {
            nuevoEstado = EstadoCuenta.valueOf(payload.nuevoEstado().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw payloadInvalido("UsuarioEstadoCambiado", "estado desconocido '" + payload.nuevoEstado() + "'");
        }
        procesarEventosExternosUseCase.actualizarEstadoCuenta(payload.usuarioId(), payload.email(), nuevoEstado);
    }

    @RabbitListener(queues = "${application.messaging.queues.saved-search-synced:styleradar.notificaciones.busqueda-guardada.q}")
    public void onBusquedaGuardadaSincronizada(BusquedaGuardadaSincronizadaEventPayload payload) {
        if (payload == null || payload.busquedaId() == null || payload.usuarioId() == null) {
            throw payloadInvalido("BusquedaGuardadaSincronizada", "incompleto");
        }
        boolean eliminada = "ELIMINADA".equalsIgnoreCase(payload.accion());
        CriterioBusquedaVO criterios = new CriterioBusquedaVO(
                payload.textoLibre(),
                payload.categoria(),
                payload.color(),
                payload.talla(),
                payload.marca(),
                payload.estilo(),
                payload.precioMin(),
                payload.precioMax(),
                payload.localidad()
        );
        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia(
                payload.busquedaId(),
                payload.usuarioId(),
                payload.nombre() != null ? payload.nombre() : "Busqueda",
                criterios,
                !eliminada,
                payload.fechaEvento() != null ? payload.fechaEvento() : LocalDateTime.now()
        );
        procesarEventosExternosUseCase.sincronizarBusquedaGuardada(copia, eliminada);
    }

    @RabbitListener(queues = "${application.messaging.queues.message-sent:styleradar.notificaciones.mensaje-enviado.q}")
    public void onMensajeEnviado(MensajeEnviadoEventPayload payload) {
        if (payload == null || payload.destinatarioId() == null || payload.conversacionId() == null) {
            throw payloadInvalido("MensajeEnviado", "incompleto");
        }
        String remitente = payload.nombreRemitente() != null ? payload.nombreRemitente() : "Un usuario";
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.destinatarioId(),
                TipoNotificacion.MENSAJE_NUEVO,
                "Nuevo mensaje de " + remitente,
                payload.resumenContenido() != null ? payload.resumenContenido() : "Tienes un nuevo mensaje en el chat.",
                "/chat/conversaciones/" + payload.conversacionId(),
                null
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.donation-changed:styleradar.notificaciones.donacion-cambiada.q}")
    public void onDonacionEstadoCambiado(DonacionEstadoCambiadoEventPayload payload) {
        if (payload == null || payload.donanteId() == null || payload.donacionId() == null) {
            throw payloadInvalido("DonacionEstadoCambiado", "incompleto");
        }
        String fundacion = payload.nombreFundacion() != null ? payload.nombreFundacion() : "la fundacion aliada";
        String detalle = "Tu donacion con " + fundacion + " cambio a estado " + payload.nuevoEstado();
        if (payload.motivo() != null && !payload.motivo().isBlank()) {
            detalle += ". Motivo: " + payload.motivo();
        }
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.donanteId(),
                TipoNotificacion.CAMBIO_DONACION,
                "Actualizacion de tu donacion #" + payload.donacionId(),
                detalle,
                "/donaciones/" + payload.donacionId(),
                null
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.moderation-changed:styleradar.notificaciones.moderacion-cambiada.q}")
    public void onModeracionPrendaCambiada(ModeracionPrendaCambiadaEventPayload payload) {
        if (payload == null || payload.propietarioUsuarioId() == null || payload.itemId() == null) {
            throw payloadInvalido("ModeracionPrendaCambiada", "incompleto");
        }
        if ("RETIRADA".equalsIgnoreCase(payload.estadoModeracion())
                || "ELIMINADA".equalsIgnoreCase(payload.estadoModeracion())) {
            procesarEventosExternosUseCase.desactivarAlertasPorItemEliminado(payload.itemId());
        }
        String detalle = "La prenda '" + payload.nombrePrenda() + "' fue marcada como " + payload.estadoModeracion();
        if (payload.observacion() != null && !payload.observacion().isBlank()) {
            detalle += ": " + payload.observacion();
        }
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.propietarioUsuarioId(),
                TipoNotificacion.MODERACION_PRENDA,
                "Estado de moderacion actualizado",
                detalle,
                "/catalogo/mis-prendas/" + payload.itemId(),
                payload.itemId()
        );
    }

    private AmqpRejectAndDontRequeueException payloadInvalido(String evento, String detalle) {
        log.error("Payload de {} invalido ({}), se envia a la cola de errores sin reintentos (SR-RNF-15)", evento, detalle);
        return new AmqpRejectAndDontRequeueException("Payload de " + evento + " invalido: " + detalle);
    }
}
