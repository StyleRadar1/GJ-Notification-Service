package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de suscripción de alerta del usuario para una prenda específica (para el botón 'AVISARME CUANDO LLEGUE' del frontend)")
public record EstadoSuscripcionAlertaResponse(
        @Schema(description = "Indica si el usuario ya tiene una alerta activa para la prenda y talla consultadas", example = "true")
        boolean subscribed,

        @Schema(description = "ID de la alerta activa encontrada (null si no está suscrito)", example = "501")
        Long alertId,

        @Schema(description = "Identificador de la prenda consultada (UUID o alfanumérico)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String itemId,

        @Schema(description = "Talla objetivo de la alerta activa", example = "M")
        String tallaObjetivo,

        @Schema(description = "Canal de notificación configurado en la alerta activa (IN_APP, EMAIL o AMBOS)", example = "IN_APP")
        CanalNotificacion canal
) {
}
