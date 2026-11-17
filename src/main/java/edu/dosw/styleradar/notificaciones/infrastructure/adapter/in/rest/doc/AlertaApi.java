package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(
        name = "Alerts & Notifications",
        description = "Endpoints under /api/v1/users/{id} to manage stock alerts, saved searches and real-time notifications (with authorization validation)"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(
            summary = "Create a new alert",
            description = "Configures an alert for an out-of-stock item or saved search for the specified user (validated against JWT token)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Alert created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class),
                            examples = @ExampleObject(
                                    name = "AlertaCreadaEjemplo",
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input payload",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ValidacionFallida",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Validation Error\",\"status\":400,\"detail\":\"itemId must be positive; alert type is required\",\"instance\":\"/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: missing or expired JWT token",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"JWT token missing, invalid or expired\",\"instance\":\"/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: path ID does not match JWT principal",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AccesoDenegado",
                                    value = "{\"type\":\"https://styleradar.co/errors/403\",\"title\":\"Access Denied\",\"status\":403,\"detail\":\"No tienes autorizacion para acceder o modificar los recursos del usuario 202\",\"instance\":\"/users/202/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Active alert already exists for this item/size",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AlertaDuplicada",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"An active alert already exists for this item/size\",\"instance\":\"/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation (e.g. item has stock)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ItemConStock",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,\"detail\":\"Cannot create alert for an item that currently has stock\",\"instance\":\"/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Catalog microservice unavailable",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "CatalogoCaido",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,\"detail\":\"Catalog service did not respond\",\"instance\":\"/users/101/alerts\"}"
                            )
                    )
            )
    })
    ResponseEntity<AlertaResponse> crearAlerta(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Valid CrearAlertaRequest request
    );

    @Operation(summary = "List user alerts", description = "Retrieves stock alerts and saved searches belonging to the authenticated user")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerts retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AlertaResponse.class)),
                            examples = @ExampleObject(
                                    name = "ListaAlertas",
                                    value = "[{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: path ID does not match JWT principal",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<List<AlertaResponse>> listarMisAlertas(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id
    );

    @Operation(summary = "Deactivate alert by ID (DELETE)", description = "Sets alert status to DESACTIVADA")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alert deactivated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alert not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlerta(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID of the alert to deactivate", example = "501") Long alertId
    );

    @Operation(summary = "Deactivate alert by ID (PATCH)", description = "PATCH alias to deactivate alert")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alert deactivated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alert not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID of the alert to deactivate", example = "501") Long alertId
    );

    @Operation(
            summary = "List user notifications",
            description = "Retrieves user notifications. Without pagination parameters returns direct array. With 'page', 'size', 'read' or 'unreadOnly' returns paginated result."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of notifications (direct array or paginated structure)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "ArrayDirectoCompatibilidad",
                                            summary = "Without pagination parameters (direct array)",
                                            value = "[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}]"
                                    ),
                                    @ExampleObject(
                                            name = "EstructuraPaginada",
                                            summary = "With page, size, read, unreadOnly (paginated)",
                                            value = "{\"content\":[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1,\"first\":true,\"last\":true}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters (page < 0, size < 1 or size > 100)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<?> listarMisNotificaciones(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(description = "Filter only unread notifications (English alias)", example = "false") Boolean unreadOnly,
            @Parameter(description = "Filter only unread notifications (legacy)", example = "false") Boolean soloNoLeidas,
            @Parameter(description = "Filter by read status (true=read, false=unread)", example = "false") Boolean read,
            @Parameter(description = "Filter by read status (legacy)", example = "false") Boolean leida,
            @Parameter(description = "Page number (zero-indexed, >= 0)", example = "0") @Min(value = 0, message = "page debe ser 0 o mayor") Integer page,
            @Parameter(description = "Page size (1 to 100)", example = "20") @Min(value = 1, message = "size debe ser al menos 1") @Max(value = 100, message = "size no puede superar 100") Integer size
    );

    @Operation(summary = "Mark notification as read", description = "Updates specific notification status to read=true")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "notificationId", description = "Notification ID", example = "901") Long notificationId
    );

    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications of the user as read")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Count of remaining unread notifications",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContadorNoLeidasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id
    );

    @Operation(summary = "Get unread notifications count", description = "Returns total count of unread notifications for frontend bell badge")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Unread count retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContadorNoLeidasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id
    );

    @Operation(summary = "Subscribe to real-time notification stream (SSE)", description = "Opens a Server-Sent Events stream to receive real-time notifications without page refresh")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE stream established successfully",
                    content = @Content(mediaType = "text/event-stream")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    SseEmitter suscribirCampanaTiempoReal(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = "Last-Event-ID",
                    description = "Last received event ID. Sent by browser upon reconnect to replay missed notifications.",
                    required = false,
                    example = "42"
            ) String ultimoEventoId
    );
}
