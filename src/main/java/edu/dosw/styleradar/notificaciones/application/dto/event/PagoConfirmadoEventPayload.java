package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/** Pago confirmado por ms-pagos (plan Radar+ o cupos de almacen aliado). */
public record PagoConfirmadoEventPayload(
        String eventoId,
        Long pagoId,
        Long usuarioId,
        String plan,
        LocalDateTime fechaConfirmacion
) {

    /**
     * Acepta el formato plano y el sobre {@code data} de ms-Payment (ordenId, usuarioId o suscriptorId numerico,
     * planId). Si no llega el nombre del plan se usa "#planId".
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PagoConfirmadoEventPayload desdeJson(JsonNode nodo) {
        EventoJsonLector lector = new EventoJsonLector(nodo);
        Long usuarioId = lector.largo("usuarioId");
        if (usuarioId == null) {
            usuarioId = lector.largo("suscriptorId");
        }
        String plan = lector.texto("plan");
        if (plan == null) {
            Long planId = lector.largo("planId");
            plan = planId == null ? null : "#" + planId;
        }
        return new PagoConfirmadoEventPayload(
                lector.texto("eventoId", "eventId"),
                lector.largo("pagoId", "ordenId"),
                usuarioId,
                plan,
                lector.fecha("fechaConfirmacion", "timestamp"));
    }
}
