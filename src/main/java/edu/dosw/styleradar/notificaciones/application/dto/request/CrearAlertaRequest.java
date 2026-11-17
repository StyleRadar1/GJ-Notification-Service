package edu.dosw.styleradar.notificaciones.application.dto.request;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para crear una nueva alerta de disponibilidad o de busqueda guardada")
public record CrearAlertaRequest(
        @Schema(description = "Tipo de alerta a configurar", example = "ITEM_AGOTADO")
        @NotNull(message = "El tipo de alerta es obligatorio")
        TipoAlerta tipoAlerta,

        @Schema(description = "ID de la prenda agotada (UUID o alfanumerico; obligatorio si tipoAlerta es ITEM_AGOTADO)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @Size(max = 100, message = "El itemId no puede superar los 100 caracteres")
        String itemId,

        @Schema(description = "Talla especifica deseada (opcional)", example = "M")
        @Size(max = 20, message = "La talla objetivo no puede superar los 20 caracteres")
        String tallaObjetivo,

        @Schema(description = "ID de la busqueda guardada (obligatorio si tipoAlerta es BUSQUEDA_GUARDADA)", example = "301")
        @Positive(message = "El busquedaGuardadaId debe ser un numero positivo")
        Long busquedaGuardadaId,

        @Schema(description = "Canal de notificacion preferido", example = "IN_APP")
        CanalNotificacion canal
) {
}
