package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta con la informacion de una notificacion del usuario")
public record NotificacionResponse(
        @Schema(description = "Identificador unico de la notificacion", example = "901")
        Long id,

        @Schema(description = "Identificador del usuario destinatario", example = "101")
        Long usuarioId,

        @Schema(description = "Identificador de la alerta origen (opcional)", example = "501")
        Long alertaId,

        @Schema(description = "Identificador de la prenda asociada (opcional)", example = "1045")
        Long itemId,

        @Schema(description = "Ciclo de reposicion de stock (idempotencia)", example = "1")
        Integer cicloReposicion,

        @Schema(description = "Tipo de notificacion", example = "STOCK_REPUESTO")
        TipoNotificacion tipo,

        @Schema(description = "Canal de emision", example = "IN_APP")
        CanalNotificacion canal,

        @Schema(description = "Titulo de la notificacion", example = "Stock Disponible")
        String titulo,

        @Schema(description = "Cuerpo descriptivo del mensaje", example = "La prenda Chaqueta Denim Vintage vuelve a tener stock.")
        String mensaje,

        @Schema(description = "Enlace deep-link a la vista frontend", example = "/prendas/1045")
        String enlace,

        @Schema(description = "Indica si la notificacion ya fue leida", example = "false")
        boolean leida,

        @Schema(description = "Fecha y hora de generacion", example = "2026-11-02T12:00:00")
        LocalDateTime fechaCreacion,

        @Schema(description = "Fecha y hora de lectura por el usuario", example = "null")
        LocalDateTime fechaLectura
) {
}
