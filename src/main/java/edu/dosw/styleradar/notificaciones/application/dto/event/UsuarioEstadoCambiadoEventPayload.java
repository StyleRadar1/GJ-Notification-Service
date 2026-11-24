package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

public record UsuarioEstadoCambiadoEventPayload(
        @JsonAlias("eventId") String eventoId,
        @JsonAlias("userId") Long usuarioId,
        String email,
        @JsonAlias({"estadoNuevo", "newStatus"}) String nuevoEstado,
        @JsonAlias("changedAt") LocalDateTime fechaCambio
) {
}
