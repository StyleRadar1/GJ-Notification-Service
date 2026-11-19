package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Count of the user's unread notifications")
public record ContadorNoLeidasResponse(
        @Schema(description = "User id", example = "101")
        long usuarioId,

        @Schema(description = "Total unread notifications in the bell", example = "3")
        long cantidadNoLeidas
) {
}
