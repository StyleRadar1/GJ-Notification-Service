package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

public record UsuarioEstadoCambiadoEventPayload(
        @JsonAlias("eventId") String eventoId,
        Long usuarioId,
        String email,
        @JsonAlias("estadoNuevo") String nuevoEstado,
        LocalDateTime fechaCambio
) {
}
