package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

/**
 * Nueva solicitud de donacion dirigida a una fundacion aliada. Donaciones puede enviar el destinatario como
 * {@code representanteId} y la solicitud como {@code solicitudId}: se aceptan como equivalentes. El destinatario es
 * el id de usuario (ms-auth) del representante, no el id de la fundacion en ms-tiendas.
 */
public record SolicitudDonacionCreadaEventPayload(
        String eventoId,
        @JsonAlias("solicitudId") Long donacionId,
        @JsonAlias("representanteId") Long fundacionUsuarioId,
        String nombreDonante,
        String resumen,
        LocalDateTime fechaEvento
) {
}
