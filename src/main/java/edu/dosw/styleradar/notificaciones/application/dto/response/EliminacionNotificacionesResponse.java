package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of deleting the user's read notifications")
public record EliminacionNotificacionesResponse(
        @Schema(description = "User id", example = "101")
        long usuarioId,

        @Schema(description = "How many notifications were deleted", example = "12")
        long cantidadEliminadas
) {
}
