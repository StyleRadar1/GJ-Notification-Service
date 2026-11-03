package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con el conteo de notificaciones no leidas del usuario")
public record ContadorNoLeidasResponse(
        @Schema(description = "Identificador del usuario", example = "101")
        long usuarioId,

        @Schema(description = "Total de notificaciones no leidas en la campana", example = "3")
        long cantidadNoLeidas
) {
}
