package edu.dosw.styleradar.notificaciones.application.dto.request;

import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Canal por el que el usuario quiere recibir un tipo de aviso")
public record PreferenciaAvisoRequest(
        @Schema(description = "SOLO_PLATAFORMA o CORREO_Y_PLATAFORMA", example = "CORREO_Y_PLATAFORMA")
        @NotNull(message = "El canal es obligatorio")
        CanalAviso canal
) {
}
