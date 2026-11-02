package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record DonacionEstadoCambiadoEventPayload(
        String eventoId,
        Long donacionId,
        Long donanteId,
        String nombreFundacion,
        String nuevoEstado,
        String motivo,
        LocalDateTime fechaCambio
) {
}
