package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Canal elegido para un tipo de aviso")
public record PreferenciaAvisoResponse(
        @Schema(description = "Tipo de aviso", example = "REPOSICION_STOCK")
        TipoNotificacion tipo,

        @Schema(description = "Canal elegido; SOLO_PLATAFORMA si el usuario nunca lo configuro", example = "CORREO_Y_PLATAFORMA")
        CanalAviso canal
) {
}
