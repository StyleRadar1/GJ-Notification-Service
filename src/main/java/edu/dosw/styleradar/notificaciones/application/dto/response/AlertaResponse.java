package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "An alert configured by the user")
public record AlertaResponse(
        @Schema(description = "Unique alert id", example = "501")
        Long id,

        @Schema(description = "Id of the owner user", example = "101")
        Long usuarioId,

        @Schema(description = "Configured alert type", example = "ITEM_AGOTADO")
        TipoAlerta tipoAlerta,

        @Schema(description = "Garment id (only for ITEM_AGOTADO, UUID or text)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String itemId,

        @Schema(description = "Monitored target size", example = "M")
        String tallaObjetivo,

        @Schema(description = "Saved search id (only for BUSQUEDA_GUARDADA)", example = "null")
        Long busquedaGuardadaId,

        @Schema(description = "Name of the saved search (only for BUSQUEDA_GUARDADA); lets the front show the list without querying ms-usuarios", example = "Black bomber jackets")
        String nombreBusqueda,

        @Schema(description = "Notification channel of the alert", example = "IN_APP")
        CanalNotificacion canal,

        @Schema(description = "Current alert status", example = "ACTIVA")
        EstadoAlerta estado,

        @Schema(description = "Creation date and time", example = "2026-11-02T10:15:30")
        LocalDateTime fechaCreacion,

        @Schema(description = "Last update date and time", example = "2026-11-02T10:15:30")
        LocalDateTime fechaActualizacion
) {
}
