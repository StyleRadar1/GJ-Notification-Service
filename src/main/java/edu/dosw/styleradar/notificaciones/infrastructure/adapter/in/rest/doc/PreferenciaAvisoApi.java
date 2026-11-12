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
        name = "Preferencias de avisos",
        description = "Elegir, por tipo de aviso, si se recibe solo en la plataforma o tambien por correo"
)
@SecurityRequirement(name = "bearerAuth")
public interface PreferenciaAvisoApi {

    @Operation(
            summary = "Listar mis preferencias",
            description = "Devuelve un canal por cada tipo de aviso. Los tipos que el usuario nunca configuro salen como SOLO_PLATAFORMA"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencias del usuario autenticado",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PreferenciaAvisoResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT expirado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<List<PreferenciaAvisoResponse>> listarMisPreferencias();

    @Operation(
            summary = "Elegir el canal de un tipo de aviso",
            description = "Guarda (o cambia) el canal del tipo indicado: SOLO_PLATAFORMA o CORREO_Y_PLATAFORMA"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferencia guardada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PreferenciaAvisoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tipo de aviso o canal invalido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT expirado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<PreferenciaAvisoResponse> guardarPreferencia(
            @Parameter(description = "Tipo de aviso", example = "REPOSICION_STOCK") TipoNotificacion tipo,
            @Valid PreferenciaAvisoRequest request
    );
}
