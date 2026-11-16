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
        name = "Notification Preferences",
        description = "Manage notification delivery channels per notification type under /users/{id}/notification-preferences"
)
@SecurityRequirement(name = "bearerAuth")
public interface PreferenciaAvisoApi {

    @Operation(
            summary = "List user notification preferences",
            description = "Retrieves delivery channel preference per notification type for the specified user. Defaults to SOLO_PLATAFORMA if unset."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User notification preferences retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PreferenciaAvisoResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized or expired JWT token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: path ID does not match JWT principal",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<List<PreferenciaAvisoResponse>> listarMisPreferencias(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id
    );

    @Operation(
            summary = "Set delivery channel for notification type",
            description = "Saves or updates the delivery channel for the given notification type: SOLO_PLATAFORMA or CORREO_Y_PLATAFORMA"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preference saved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreferenciaAvisoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid notification type or channel",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized or expired JWT token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: path ID does not match JWT principal",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<PreferenciaAvisoResponse> guardarPreferencia(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "type", description = "Notification type", example = "REPOSICION_STOCK") TipoNotificacion type,
            @Valid PreferenciaAvisoRequest request
    );
}
