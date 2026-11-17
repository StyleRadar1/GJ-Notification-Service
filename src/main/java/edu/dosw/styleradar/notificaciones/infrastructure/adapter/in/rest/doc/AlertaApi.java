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
        name = "Alertas y Notificaciones",
        description = "Endpoints bajo /api/v1/users/{id} para gestionar alertas de stock, búsquedas guardadas y notificaciones en tiempo real (con validación de autorización)"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(
            summary = "Crear una nueva alerta",
            description = "Configura una alerta por ítem agotado o por búsqueda guardada para el usuario especificado (validado contra el token JWT)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Alerta creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class),
                            examples = @ExampleObject(
                                     name = "AlertaCreadaEjemplo",
                                     value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cuerpo de la solicitud inválido o campos faltantes",
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
                    description = "No autorizado: token JWT ausente, inválido o expirado",
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
                    description = "Prohibido: el ID de la ruta no coincide con el usuario autenticado",
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
                    description = "Ya existe una alerta activa para este ítem y talla",
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
                    description = "Violación de regla de negocio (ej. la prenda tiene stock disponible)",
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
                    description = "Microservicio de catálogo no disponible",
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
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Valid CrearAlertaRequest request
    );

    @Operation(summary = "Listar alertas del usuario", description = "Recupera las alertas de stock y búsquedas guardadas pertenecientes al usuario autenticado")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alertas recuperadas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AlertaResponse.class)),
                            examples = @ExampleObject(
                                    name = "ListaAlertas",
                                    value = "[{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado: token JWT ausente o expirado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido: el ID de la ruta no coincide con el usuario autenticado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<List<AlertaResponse>> listarMisAlertas(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id
    );

    @Operation(summary = "Desactivar alerta por ID (DELETE)", description = "Cambia el estado de la alerta a DESACTIVADA")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta no encontrada",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlerta(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID de la alerta a desactivar", example = "501") Long alertId
    );

    @Operation(summary = "Desactivar alerta por ID (PATCH)", description = "Ruta alternativa PATCH para desactivar la alerta")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AlertaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta no encontrada",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID de la alerta a desactivar", example = "501") Long alertId
    );

    @Operation(
            summary = "Listar notificaciones del usuario",
            description = "Recupera las notificaciones del usuario. Sin parámetros de paginación devuelve el arreglo directo. Con 'page', 'size', 'read' o 'unreadOnly' devuelve resultado paginado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de notificaciones (arreglo directo o estructura paginada)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "ArrayDirectoCompatibilidad",
                                            summary = "Sin parámetros de paginación (arreglo directo)",
                                            value = "[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}]"
                                    ),
                                    @ExampleObject(
                                            name = "EstructuraPaginada",
                                            summary = "Con page, size, read o unreadOnly (paginado)",
                                            value = "{\"content\":[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1,\"first\":true,\"last\":true}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de paginación inválidos (page < 0, size < 1 o size > 100)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<?> listarMisNotificaciones(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(description = "Filtrar solo no leídas (alias en inglés)", example = "false") Boolean unreadOnly,
            @Parameter(description = "Filtrar solo no leídas (alias en español)", example = "false") Boolean soloNoLeidas,
            @Parameter(description = "Filtrar por estado de lectura (true=leídas, false=no leídas)", example = "false") Boolean read,
            @Parameter(description = "Filtrar por estado de lectura (alias en español)", example = "false") Boolean leida,
            @Parameter(description = "Número de página (iniciando en 0, >= 0)", example = "0") @Min(value = 0, message = "page debe ser 0 o mayor") Integer page,
            @Parameter(description = "Tamaño de página (1 a 100)", example = "20") @Min(value = 1, message = "size debe ser al menos 1") @Max(value = 100, message = "size no puede superar 100") Integer size
    );

    @Operation(summary = "Marcar notificación como leída", description = "Actualiza el estado de la notificación específica a leída (read=true)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificación marcada como leída exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notificación no encontrada",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "notificationId", description = "ID de la notificación a marcar como leída", example = "901") Long notificationId
    );

    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Marca como leídas todas las notificaciones pendientes del usuario")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conteo de notificaciones no leídas restantes (0)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContadorNoLeidasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id
    );

    @Operation(summary = "Obtener conteo de notificaciones no leídas", description = "Devuelve el total de notificaciones no leídas para la insignia de la campana en el frontend")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conteo de no leídas obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContadorNoLeidasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id
    );

    @Operation(summary = "Suscribirse al flujo de notificaciones en tiempo real (SSE)", description = "Abre un flujo Server-Sent Events para recibir notificaciones en tiempo real sin recargar la página")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flujo SSE establecido exitosamente",
                    content = @Content(mediaType = "text/event-stream")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Prohibido",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    SseEmitter suscribirCampanaTiempoReal(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = "Last-Event-ID",
                    description = "ID del último evento recibido. Enviado por el navegador al reconectarse para reproducir notificaciones pendientes.",
                    required = false,
                    example = "42"
            ) String ultimoEventoId
    );
}
