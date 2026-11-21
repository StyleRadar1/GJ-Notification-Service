package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Channel chosen for a notification type")
public record PreferenciaAvisoResponse(
        @Schema(description = "Notification type", example = "REPOSICION_STOCK")
        TipoNotificacion tipo,

        @Schema(description = "Chosen channel; SOLO_PLATAFORMA if the user never configured it", example = "CORREO_Y_PLATAFORMA")
        CanalAviso canal
) {
}
