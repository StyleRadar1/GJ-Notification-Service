package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Si el usuario ya tiene una alerta activa para una prenda y talla")
public record SuscripcionAlertaResponse(
        @Schema(description = "true si ya hay una alerta ACTIVA para la prenda y la talla", example = "true")
        boolean suscrita,

        @Schema(description = "Id de la alerta activa (null si no esta suscrito); sirve para desactivarla", example = "501")
        Long alertaId,

        @Schema(description = "Prenda consultada", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String itemId,

        @Schema(description = "Talla consultada (null si se consulto sin talla)", example = "M")
        String tallaObjetivo,

        @Schema(description = "Canal de la alerta activa (null si no esta suscrito)", example = "IN_APP")
        CanalNotificacion canal
) {
}
