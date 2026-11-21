package edu.dosw.styleradar.notificaciones.application.dto.request;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new availability alert or saved-search alert")
public record CrearAlertaRequest(
        @Schema(description = "Type of alert to configure", example = "ITEM_AGOTADO")
        @NotNull(message = "El tipo de alerta es obligatorio")
        TipoAlerta tipoAlerta,

        @Schema(description = "Id of the out-of-stock garment (UUID or text; required when tipoAlerta is ITEM_AGOTADO)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @Size(max = 100, message = "El itemId no puede superar los 100 caracteres")
        String itemId,

        @Schema(description = "Specific size wanted (optional)", example = "M")
        @Size(max = 20, message = "La talla objetivo no puede superar los 20 caracteres")
        String tallaObjetivo,

        @Schema(description = "Id of the saved search (required when tipoAlerta is BUSQUEDA_GUARDADA)", example = "301")
        @Positive(message = "El busquedaGuardadaId debe ser un numero positivo")
        Long busquedaGuardadaId,

        @Schema(description = "Preferred notification channel: IN_APP, EMAIL, PUSH or AMBOS (platform and email)", example = "IN_APP")
        CanalNotificacion canal
) {
}
