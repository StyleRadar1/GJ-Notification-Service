package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record PrendaRetiradaEventPayload(
        String eventoId,
        Long itemId,
        String motivo,
        LocalDateTime fechaEvento
) {
}
