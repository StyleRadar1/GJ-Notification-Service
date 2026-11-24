package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.EliminacionNotificacionesResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.SuscripcionAlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.TicketStreamResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
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
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"nombreBusqueda\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Error de validacion en la solicitud\",\"status\":400,\"code\":\"VALIDATION_ERROR\",\"detail\":\"El tipo de alerta es obligatorio\",\"instance\":\"/api/v1/users/101/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"El tipo de alerta es obligatorio\",\"errors\":{\"tipoAlerta\":\"El tipo de alerta es obligatorio\"}}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"No autenticado\",\"status\":401,\"code\":\"UNAUTHENTICATED\",\"detail\":\"Token JWT ausente, invalido o expirado\",\"instance\":\"/api/v1/users/101/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"Token JWT ausente, invalido o expirado\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Acceso denegado\",\"status\":403,\"code\":\"ACCESS_DENIED\",\"detail\":\"No tienes autorizacion para acceder o modificar los recursos de otro usuario\",\"instance\":\"/api/v1/users/202/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"No tienes autorizacion para acceder o modificar los recursos de otro usuario\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Conflicto de estado del recurso\",\"status\":409,\"code\":\"ALERT_ALREADY_ACTIVE\",\"detail\":\"Ya existe una alerta activa configurada para este recurso\",\"instance\":\"/api/v1/users/101/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"Ya existe una alerta activa configurada para este recurso\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Violacion de regla de negocio\",\"status\":422,\"code\":\"ITEM_HAS_STOCK\",\"detail\":\"No se puede crear una alerta de reposicion porque la prenda actualmente tiene stock disponible\",\"instance\":\"/api/v1/users/101/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"No se puede crear una alerta de reposicion porque la prenda actualmente tiene stock disponible\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Servicio externo no disponible\",\"status\":503,\"code\":\"CATALOG_UNAVAILABLE\",\"detail\":\"El catalogo no esta disponible en este momento, intente nuevamente mas tarde\",\"instance\":\"/api/v1/users/101/alerts\",\"timestamp\":\"2026-11-17T15:30:00Z\",\"mensaje\":\"El catalogo no esta disponible en este momento, intente nuevamente mas tarde\"}"
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
                                    value = "[{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"nombreBusqueda\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}]"
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
            summary = "Check whether the user already has an alert for a garment",
            description = "Lightweight lookup for the out-of-stock garment page: returns suscrita=true and the alertId when the user "
                    + "has an ACTIVE out-of-stock alert for the given itemId and size (tallaObjetivo, optional), so the front can "
                    + "paint the alert button without downloading the whole list."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subscription state",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionAlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "itemId missing",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<SuscripcionAlertaResponse> comprobarSuscripcion(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "itemId", description = "Garment id (UUID or text)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String itemId,
            @Parameter(name = "tallaObjetivo", description = "Size (optional)", example = "M") String tallaObjetivo
    );

    @Operation(
            summary = "Reactivate an alert",
            description = "Sets a deactivated alert back to ACTIVE. Applies the same rules as creating it: the account must "
                    + "not be inactive, there must be no other active identical alert (409), and the garment must still be "
                    + "out of stock or the saved search still active (422). An already active alert is returned unchanged."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alert is active",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Alert belongs to another user",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alert not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "There is already an active alert for the same garment or saved search",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation (account inactive, garment has stock, saved search inactive)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Catalog microservice unavailable",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> activarAlerta(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID of the alert to reactivate", example = "501") Long alertId
    );

    @Operation(
            summary = "List user notifications",
            description = "Retrieves user notifications, most recent first. Without 'page', 'size' or 'read' it returns a direct array. With any of them it returns a page (content, page, size, totalElements, totalPages, hasNext, hasPrevious). 'read=false' returns only unread notifications."
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
                                            value = "[{\"id\":901,\"usuarioId\":101,\"alertaId\":501,\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"cicloReposicion\":1,\"tipo\":\"REPOSICION_STOCK\",\"canal\":\"IN_APP\",\"titulo\":\"Prenda disponible nuevamente\",\"mensaje\":\"Chaqueta Bomber Oversize Negra volvio a tener unidades en la talla M\",\"enlace\":\"/catalogo/prendas/3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"fechaLectura\":null}]"
                                    ),
                                    @ExampleObject(
                                            name = "EstructuraPaginada",
                                            summary = "With page, size or read (paginated)",
                                            value = "{\"content\":[{\"id\":901,\"usuarioId\":101,\"alertaId\":501,\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"cicloReposicion\":1,\"tipo\":\"REPOSICION_STOCK\",\"canal\":\"IN_APP\",\"titulo\":\"Prenda disponible nuevamente\",\"mensaje\":\"Chaqueta Bomber Oversize Negra volvio a tener unidades en la talla M\",\"enlace\":\"/catalogo/prendas/3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"fechaLectura\":null}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1,\"hasNext\":false,\"hasPrevious\":false}"
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
            @Parameter(hidden = true) Boolean unreadOnly,
            @Parameter(hidden = true) Boolean soloNoLeidas,
            @Parameter(description = "Single read-status filter: true = read, false = unread. Activates the paginated response", example = "false") Boolean read,
            @Parameter(hidden = true) Boolean leida,
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

    @Operation(summary = "Delete a notification", description = "Removes one notification from the user's inbox (soft delete). A second call on the same id returns 404")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: the notification belongs to another user",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification not found or already deleted",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<Void> eliminarNotificacion(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id,
            @Parameter(name = "notificationId", description = "Notification id", example = "5001") Long notificationId
    );

    @Operation(summary = "Delete notifications in bulk", description = "Removes several notifications from the user's inbox (soft delete). Send exactly one of: read=true (every read notification; unread ones are kept) or ids (1 to 100 comma-separated notification ids, read or unread). Ids that do not exist, belong to another user or were already deleted are ignored, so repeating the call returns 0")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications deleted; cantidadEliminadas is how many were actually removed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EliminacionNotificacionesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Send exactly one of read=true or ids; ids must be positive numbers, at most 100 distinct",
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
    ResponseEntity<EliminacionNotificacionesResponse> eliminarNotificaciones(
            @Parameter(name = "id", description = "User ID resource owner", example = "101") String id,
            @Parameter(description = "Must be true: deletes every read notification. Do not combine with ids", example = "true") Boolean read,
            @Parameter(description = "Comma-separated ids of the notifications to delete (1 to 100 distinct, read or unread). Do not combine with read",
                    example = "5001,5002,5003", explode = Explode.FALSE,
                    array = @ArraySchema(schema = @Schema(type = "integer", format = "int64"))) List<Long> ids
    );

    @Operation(summary = "Get unread notifications count", description = "Returns the total count of unread notifications for the frontend bell badge")
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

    @Operation(
            summary = "Get a short-lived ticket to open the SSE stream",
            description = "The browser EventSource cannot send the Authorization header. Call this endpoint with the "
                    + "normal Bearer JWT, then open the stream with ?ticket=<ticket>. The ticket lasts seconds and "
                    + "only opens the stream; a session JWT in the URL is rejected."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket issued",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketStreamResponse.class))
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
                    responseCode = "429",
                    description = "Too many requests",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<TicketStreamResponse> emitirTicketStream(
            @Parameter(name = "id", description = "User ID resource owner (or alias 'me')", example = "101") String id
    );
}
