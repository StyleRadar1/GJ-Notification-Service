package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "A notification of the user")
public record NotificacionResponse(
        @Schema(description = "Unique notification id", example = "901")
        Long id,

        @Schema(description = "Id of the recipient user", example = "101")
        Long usuarioId,

        @Schema(description = "Id of the alert that originated it (optional)", example = "501")
        Long alertaId,

        @Schema(description = "Id of the related garment (UUID or text, optional)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String itemId,

        @Schema(description = "Stock replenishment cycle (idempotency)", example = "1")
        Integer cicloReposicion,

        @Schema(description = "Notification type", example = "REPOSICION_STOCK")
        TipoNotificacion tipo,

        @Schema(description = "Delivery channel", example = "IN_APP")
        CanalNotificacion canal,

        @Schema(description = "Notification title", example = "Stock Disponible")
        String titulo,

        @Schema(description = "Descriptive message body", example = "La prenda Chaqueta Denim Vintage vuelve a tener stock.")
        String mensaje,

        @Schema(description = "Deep link to the frontend view", example = "/catalogo/prendas/1045")
        String enlace,

        @Schema(description = "Whether the notification has been read", example = "false")
        boolean leida,

        @Schema(description = "Generation date and time", example = "2026-11-02T12:00:00")
        LocalDateTime fechaCreacion,

        @Schema(description = "Date and time the user read it", example = "null")
        LocalDateTime fechaLectura
) {
}
