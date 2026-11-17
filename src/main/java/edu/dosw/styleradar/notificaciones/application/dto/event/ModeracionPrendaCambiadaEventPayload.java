package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record ModeracionPrendaCambiadaEventPayload(
        String eventoId,
        String itemId,
        Long propietarioUsuarioId,
        String nombrePrenda,
        String estadoModeracion,
        String observacion,
        LocalDateTime fechaModeracion
) {
}
