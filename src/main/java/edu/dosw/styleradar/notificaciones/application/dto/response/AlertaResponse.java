package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta con la informacion de una alerta configurada por el usuario")
public record AlertaResponse(
        @Schema(description = "Identificador unico de la alerta", example = "501")
        Long id,

        @Schema(description = "Identificador del usuario propietario", example = "101")
        Long usuarioId,

        @Schema(description = "Tipo de alerta configurada", example = "ITEM_AGOTADO")
        TipoAlerta tipoAlerta,

        @Schema(description = "Identificador de la prenda (solo para ITEM_AGOTADO)", example = "1045")
        Long itemId,

        @Schema(description = "Talla objetivo monitoreada", example = "M")
        String tallaObjetivo,

        @Schema(description = "Identificador de la busqueda guardada (solo para BUSQUEDA_GUARDADA)", example = "null")
        Long busquedaGuardadaId,

        @Schema(description = "Canal de recepcion de la notificacion", example = "IN_APP")
        CanalNotificacion canal,

        @Schema(description = "Estado actual de la alerta", example = "ACTIVA")
        EstadoAlerta estado,

        @Schema(description = "Fecha y hora de creacion", example = "2026-11-02T10:15:30")
        LocalDateTime fechaCreacion,

        @Schema(description = "Fecha y hora de ultima actualizacion", example = "2026-11-02T10:15:30")
        LocalDateTime fechaActualizacion
) {
}
