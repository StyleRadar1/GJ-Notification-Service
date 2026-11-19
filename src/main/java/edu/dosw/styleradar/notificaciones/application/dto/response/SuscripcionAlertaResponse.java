package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the user already has an active alert for a garment and size")
public record SuscripcionAlertaResponse(
        @Schema(description = "true if there is already an ACTIVE alert for the garment and size", example = "true")
        boolean suscrita,

        @Schema(description = "Id of the active alert (null if not subscribed); useful to deactivate it", example = "501")
        Long alertaId,

        @Schema(description = "Garment checked", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String itemId,

        @Schema(description = "Size checked (null when checked without size)", example = "M")
        String tallaObjetivo,

        @Schema(description = "Channel of the active alert (null if not subscribed)", example = "IN_APP")
        CanalNotificacion canal
) {
}
