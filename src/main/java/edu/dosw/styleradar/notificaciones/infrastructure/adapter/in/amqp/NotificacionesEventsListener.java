package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp;

import edu.dosw.styleradar.notificaciones.application.dto.event.BusquedaGuardadaSincronizadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.DonacionEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionDesactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionReactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.MensajeEnviadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.ModeracionPrendaCambiadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PagoConfirmadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaPublicadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaRetiradaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaVendidaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.SolicitudDonacionCreadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        if (payload == null || payload.itemId() == null || payload.itemId().isBlank()) {
            throw payloadInvalido("InventarioActualizado", "sin itemId");
        }
        if (payload.stockActual() == null) {
            throw payloadInvalido("InventarioActualizado", "sin stockActual");
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
        if (payload == null || payload.itemId() == null || payload.itemId().isBlank()) {
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
        EstadoCuenta nuevoEstado = parsearEstadoCuenta(payload.nuevoEstado());
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
                null,
                payload.eventoId()
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.donation-changed:styleradar.notificaciones.donacion-cambiada.q}")
    public void onDonacionEstadoCambiado(DonacionEstadoCambiadoEventPayload payload) {
        if (payload == null || payload.donanteId() == null || payload.donacionId() == null) {
            throw payloadInvalido("DonacionEstadoCambiado", "incompleto");
        }
        String fundacion = payload.nombreFundacion() != null ? payload.nombreFundacion() : "la fundacion aliada";
        String detalle = detalleDeDonacion(payload.nuevoEstado(), fundacion, payload.motivo());
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.donanteId(),
                TipoNotificacion.CAMBIO_DONACION,
                "Actualizacion de tu donacion #" + payload.donacionId(),
                detalle,
                "/donaciones/" + payload.donacionId(),
                null,
                payload.eventoId()
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.moderation-changed:styleradar.notificaciones.moderacion-cambiada.q}")
    public void onModeracionPrendaCambiada(ModeracionPrendaCambiadaEventPayload payload) {
        if (payload == null || payload.itemId() == null || payload.itemId().isBlank()) {
            throw payloadInvalido("ModeracionPrendaCambiada", "sin itemId");
        }
        if ("RETIRADA".equalsIgnoreCase(payload.estadoModeracion())
                || "ELIMINADA".equalsIgnoreCase(payload.estadoModeracion())) {
            procesarEventosExternosUseCase.desactivarAlertasPorItemEliminado(payload.itemId());
        }
        if (payload.propietarioUsuarioId() == null) {
            log.warn("ModeracionPrendaCambiada del item {} sin propietarioUsuarioId, se omite la notificacion",
                    payload.itemId());
            return;
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
                payload.itemId(),
                payload.eventoId()
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.garment-deleted:styleradar.notificaciones.prenda-retirada.q}")
    public void onPrendaRetirada(PrendaRetiradaEventPayload payload) {
        if (payload == null || payload.itemId() == null || payload.itemId().isBlank()) {
            throw payloadInvalido("PrendaRetirada", "sin itemId");
        }
        procesarEventosExternosUseCase.desactivarAlertasPorItemEliminado(payload.itemId());
    }

    @RabbitListener(queues = "${application.messaging.queues.donation-requested:styleradar.notificaciones.solicitud-donacion.q}")
    public void onSolicitudDonacionCreada(SolicitudDonacionCreadaEventPayload payload) {
        if (payload == null || payload.fundacionUsuarioId() == null || payload.donacionId() == null) {
            throw payloadInvalido("SolicitudDonacionCreada", "incompleto");
        }
        String donante = payload.nombreDonante() != null ? payload.nombreDonante() : "Una persona";
        String detalle = donante + " quiere donar a tu fundacion";
        if (payload.resumen() != null && !payload.resumen().isBlank()) {
            detalle += ": " + payload.resumen();
        }
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.fundacionUsuarioId(),
                TipoNotificacion.SOLICITUD_DONACION,
                "Nueva solicitud de donacion #" + payload.donacionId(),
                detalle,
                "/donaciones/" + payload.donacionId(),
                null,
                payload.eventoId()
        );
    }

    @RabbitListener(queues = "${application.messaging.queues.garment-sold:styleradar.notificaciones.prenda-vendida.q}")
    public void onPrendaVendida(PrendaVendidaEventPayload payload) {
        if (payload == null || payload.publicacionId() == null || payload.publicacionId().isBlank()) {
            throw payloadInvalido("PrendaVendida", "sin publicacionId");
        }
        String prenda = payload.nombrePrenda() != null ? payload.nombrePrenda() : "La prenda";
        List<Long> interesados = payload.interesadosIds() != null ? payload.interesadosIds() : List.of();
        for (Long interesadoId : interesados) {
            if (interesadoId == null || interesadoId.equals(payload.compradorId()) || interesadoId.equals(payload.vendedorId())) {
                continue;
            }
            procesarEventosExternosUseCase.generarNotificacionDirecta(
                    interesadoId,
                    TipoNotificacion.PRENDA_VENDIDA,
                    "Una prenda que te interesaba ya no esta disponible",
                    prenda + " se vendio y ya no esta disponible.",
                    "/segunda-mano/" + payload.publicacionId(),
                    null,
                    payload.eventoId()
            );
        }
    }

    @RabbitListener(queues = "${application.messaging.queues.payment-confirmed:styleradar.notificaciones.pago-confirmado.q}")
    public void onPagoConfirmado(PagoConfirmadoEventPayload payload) {
        if (payload == null || payload.usuarioId() == null) {
            throw payloadInvalido("PagoConfirmado", "sin usuarioId");
        }
        try {
            procesarEventosExternosUseCase.generarNotificacionPagoConfirmado(
                    payload.usuarioId(), payload.plan(), payload.eventoId());
        } catch (RecursoNoEncontradoException ex) {
            log.error("PagoConfirmado de un usuario desconocido ({}), se envia a la cola de errores", ex.getMessage());
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = "${application.messaging.queues.foundation-deactivated:styleradar.notificaciones.fundacion-desactivada.q}")
    public void onFundacionDesactivada(FundacionDesactivadaEventPayload payload) {
        if (payload == null || payload.fundacionId() == null
                || payload.eventoId() == null || payload.eventoId().isBlank()) {
            throw payloadInvalido("FundacionDesactivada", "sin fundacionId o eventoId");
        }
        Set<Long> destinatarios = new LinkedHashSet<>();
        if (payload.donantesAfectadosIds() != null) {
            for (Long donanteId : payload.donantesAfectadosIds()) {
                if (donanteId != null) {
                    destinatarios.add(donanteId);
                }
            }
        }
        if (payload.representanteId() != null) {
            destinatarios.add(payload.representanteId());
        }
        if (destinatarios.isEmpty()) {
            log.warn("FundacionDesactivada {} sin destinatarios (donantes ni representante), no hay a quien avisar",
                    payload.fundacionId());
            return;
        }
        String fundacion = textoOValorPorDefecto(payload.nombreFundacion(), "una fundacion aliada");
        String mensaje = "La fundacion " + fundacion + " ha sido desactivada" + sufijoMotivo(payload.motivo());
        for (Long destinatarioId : destinatarios) {
            // La clave de idempotencia es (eventoId, destinatarioId): un evento repetido no duplica el aviso.
            procesarEventosExternosUseCase.generarNotificacionDirecta(
                    destinatarioId,
                    TipoNotificacion.CAMBIO_DONACION,
                    "Fundacion desactivada",
                    mensaje,
                    "/donaciones",
                    null,
                    payload.eventoId()
            );
        }
    }

    @RabbitListener(queues = "${application.messaging.queues.foundation-reactivated:styleradar.notificaciones.fundacion-reactivada.q}")
    public void onFundacionReactivada(FundacionReactivadaEventPayload payload) {
        if (payload == null || payload.fundacionId() == null
                || payload.eventoId() == null || payload.eventoId().isBlank()) {
            throw payloadInvalido("FundacionReactivada", "sin fundacionId o eventoId");
        }
        if (payload.representanteId() == null) {
            log.warn("FundacionReactivada {} sin representanteId, no hay a quien avisar", payload.fundacionId());
            return;
        }
        String fundacion = textoOValorPorDefecto(payload.nombreFundacion(), "una fundacion aliada");
        String mensaje = "La fundacion " + fundacion + " ha sido reactivada y vuelve a recibir donaciones"
                + sufijoMotivo(payload.motivo());
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                payload.representanteId(),
                TipoNotificacion.CAMBIO_DONACION,
                "Fundacion reactivada",
                mensaje,
                "/donaciones",
                null,
                payload.eventoId()
        );
    }

    private EstadoCuenta parsearEstadoCuenta(String estadoCrudo) {
        String normalizado = estadoCrudo.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "ACTIVO", "ACTIVA", "ACTIVE" -> EstadoCuenta.ACTIVO;
            case "INACTIVO", "INACTIVA", "INACTIVE" -> EstadoCuenta.INACTIVO;
            case "BLOQUEADO", "BLOQUEADA", "BLOCKED" -> EstadoCuenta.BLOQUEADO;
            case "SUSPENDIDO", "SUSPENDIDA", "SUSPENDED" -> EstadoCuenta.SUSPENDIDO;
            default -> throw payloadInvalido("UsuarioEstadoCambiado", "estado desconocido '" + estadoCrudo + "'");
        };
    }

    private static String textoOValorPorDefecto(String valor, String defecto) {
        return (valor != null && !valor.isBlank()) ? valor : defecto;
    }

    private static String sufijoMotivo(String motivo) {
        return (motivo != null && !motivo.isBlank()) ? ": " + motivo : "";
    }

    private String detalleDeDonacion(String estado, String fundacion, String motivo) {
        String normalizado = estado != null ? estado.toUpperCase() : "";
        String detalle;
        if (normalizado.contains("APROB")) {
            detalle = "Tu donacion con " + fundacion + " fue aprobada";
        } else if (normalizado.contains("RECHAZ")) {
            detalle = "Tu donacion con " + fundacion + " fue rechazada";
        } else if (normalizado.contains("CITA")) {
            detalle = "Se programo una cita para entregar tu donacion a " + fundacion;
        } else {
            detalle = "Tu donacion con " + fundacion + " cambio a estado " + estado;
        }
        if (motivo != null && !motivo.isBlank()) {
            detalle += ". Motivo: " + motivo;
        }
        return detalle;
    }

    private AmqpRejectAndDontRequeueException payloadInvalido(String evento, String detalle) {
        log.error("Payload de {} invalido ({}), se envia a la cola de errores sin reintentos (SR-RNF-15)", evento, detalle);
        return new AmqpRejectAndDontRequeueException("Payload de " + evento + " invalido: " + detalle);
    }
}
