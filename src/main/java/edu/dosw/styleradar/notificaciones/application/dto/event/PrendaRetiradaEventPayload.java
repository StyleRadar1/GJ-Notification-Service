package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record PrendaRetiradaEventPayload(
        String eventoId,
        String itemId,
        String motivo,
        LocalDateTime fechaEvento
) {
}
