package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record UsuarioEstadoCambiadoEventPayload(
        String eventoId,
        Long usuarioId,
        String email,
        String nuevoEstado,
        LocalDateTime fechaCambio
) {
}
