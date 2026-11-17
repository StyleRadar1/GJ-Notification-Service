package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.PreferenciaAvisoRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.PreferenciaAvisoResponse;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Preferencias de Notificación",
        description = "Administración de canales de recepción por tipo de notificación bajo /api/v1/users/{id}/notification-preferences"
)
@SecurityRequirement(name = "bearerAuth")
public interface PreferenciaAvisoApi {

    @Operation(
            summary = "Listar preferencias de notificación del usuario",
            description = "Obtiene la preferencia de canal de entrega por cada tipo de notificación para el usuario especificado. Por defecto es SOLO_PLATAFORMA si no está configurada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencias de notificación del usuario obtenidas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PreferenciaAvisoResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado: token JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido: el ID de la ruta no coincide con el usuario autenticado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<List<PreferenciaAvisoResponse>> listarMisPreferencias(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id
    );

    @Operation(
            summary = "Configurar canal de entrega por tipo de notificación",
            description = "Guarda o actualiza el canal de recepción para el tipo de notificación especificado: SOLO_PLATAFORMA o CORREO_Y_PLATAFORMA"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencia guardada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreferenciaAvisoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tipo de notificación o canal no válido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado: token JWT ausente, inválido o expirado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido: el ID de la ruta no coincide con el usuario autenticado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<PreferenciaAvisoResponse> guardarPreferencia(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "type", description = "Tipo de notificación a configurar", example = "REPOSICION_STOCK") TipoNotificacion type,
            @Valid PreferenciaAvisoRequest request
    );
}
