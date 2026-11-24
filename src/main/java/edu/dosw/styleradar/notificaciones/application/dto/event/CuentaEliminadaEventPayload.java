package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

/** Cuenta eliminada por ms-Users ({@code userId} y {@code deletedAt} en su formato actual). */
public record CuentaEliminadaEventPayload(
        String eventoId,
        Long usuarioId,
        LocalDateTime fechaEliminacion
) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CuentaEliminadaEventPayload desdeJson(JsonNode nodo) {
        EventoJsonLector lector = new EventoJsonLector(nodo);
        return new CuentaEliminadaEventPayload(
                lector.texto("eventoId", "eventId"),
                lector.largo("usuarioId", "userId"),
                lector.fecha("fechaEliminacion", "deletedAt", "timestamp"));
    }
}
