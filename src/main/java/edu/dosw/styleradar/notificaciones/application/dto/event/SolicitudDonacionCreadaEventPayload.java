package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

/** Nueva solicitud de donacion dirigida a una fundacion aliada. */
public record SolicitudDonacionCreadaEventPayload(
        String eventoId,
        Long donacionId,
        Long fundacionUsuarioId,
        String nombreDonante,
        String resumen,
        LocalDateTime fechaEvento
) {
}
