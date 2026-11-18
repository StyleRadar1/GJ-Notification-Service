package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.EstadoSuscripcionAlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import io.swagger.v3.oas.annotations.Hidden;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(
        name = "Alerts & Notifications",
        description = "Endpoints bajo /api/v1/users/{id} alineados con el frontend: botón 'AVISARME CUANDO LLEGUE', sección 'Mis búsquedas y alertas' (en perfil y campana) y campana reactiva con burbuja morada de no leídas"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(
            summary = "Crear alerta de prenda agotada o búsqueda guardada (Botón 'AVISARME CUANDO LLEGUE')",
            description = "Configura una alerta desde el botón 'AVISARME CUANDO LLEGUE' (permitiendo seleccionar canal IN_APP o AMBOS mediante el checkbox de copia por correo) o para una búsqueda guardada. Si la prenda ya tiene stock disponible en catálogo, responde 422 para que el frontend muestre 'Esta prenda ya tiene unidades, puedes reservarla ahora' junto con el botón 'RESERVAR EN TIENDA'. Si ocurre un fallo temporal (503), el frontend muestra 'No pudimos crear la alerta, intenta más tarde' con opción de reintentar."
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Validation Error\",\"status\":400,\"detail\":\"itemId must be provided; alert type is required\",\"instance\":\"/api/v1/users/101/alerts\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"JWT token missing, invalid or expired\",\"instance\":\"/api/v1/users/101/alerts\"}"
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
                                    value = "{\"type\":\"https://styleradar.co/errors/403\",\"title\":\"Access Denied\",\"status\":403,\"detail\":\"No tienes autorizacion para acceder o modificar los recursos del usuario 202\",\"instance\":\"/api/v1/users/202/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una alerta activa para esta prenda y talla",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AlertaDuplicada",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Conflict\",\"status\":409,\"detail\":\"An active alert already exists for this item/size\",\"instance\":\"/api/v1/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "La prenda ya tiene unidades disponibles (el frontend muestra 'Esta prenda ya tiene unidades, puedes reservarla ahora' y el botón 'RESERVAR EN TIENDA')",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ItemConStock",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Unprocessable Entity\",\"status\":422,\"detail\":\"Cannot create alert for an item that currently has stock\",\"instance\":\"/api/v1/users/101/alerts\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Fallo al consultar el servicio de catálogo (el frontend muestra 'No pudimos crear la alerta, intenta más tarde' y el botón de reintentar)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "CatalogoCaido",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,\"detail\":\"Catalog service did not respond\",\"instance\":\"/api/v1/users/101/alerts\"}"
                            )
                    )
            )
    })
    ResponseEntity<AlertaResponse> crearAlerta(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Valid CrearAlertaRequest request
    );

    @Operation(
            summary = "Listar mis búsquedas y alertas",
            description = "Recupera las prendas agotadas que el usuario espera y sus búsquedas guardadas para la sección 'Mis búsquedas y alertas' (ubicada tanto en el perfil del usuario como al final del menú desplegable de la campana)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de búsquedas y alertas recuperada exitosamente",
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

    @Operation(
            summary = "Comprobar si el usuario ya tiene una alerta activa para una prenda",
            description = "Endpoint ligero para la vista de detalle de prenda agotada. Permite al frontend saber de inmediato si debe mostrar el botón 'AVISARME CUANDO LLEGUE' (subscribed=false) o indicar que la alerta ya está activa con su alertId (subscribed=true), sin tener que descargar todas las alertas del usuario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado de suscripción verificado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EstadoSuscripcionAlertaResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Suscrito",
                                            summary = "Usuario ya tiene alerta activa",
                                            value = "{\"subscribed\":true,\"alertId\":501,\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"canal\":\"IN_APP\"}"
                                    ),
                                    @ExampleObject(
                                            name = "NoSuscrito",
                                            summary = "Usuario no tiene alerta activa (mostrar botón 'AVISARME CUANDO LLEGUE')",
                                            value = "{\"subscribed\":false,\"alertId\":null,\"itemId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"tallaObjetivo\":\"M\",\"canal\":null}"
                                    )
                            }
                    )
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
    ResponseEntity<EstadoSuscripcionAlertaResponse> verificarSuscripcionAlerta(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "itemId", description = "ID de la prenda a verificar (UUID o alfanumérico)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String itemId,
            @Parameter(name = "size", description = "Talla específica a verificar (opcional)", required = false, example = "M") String size,
            @Parameter(hidden = true) String talla
    );

    @Operation(
            summary = "Eliminar o desactivar alerta o búsqueda en seguimiento",
            description = "Permite al usuario cancelar o eliminar una alerta de prenda agotada que espera o una búsqueda guardada desde la sección 'Mis búsquedas y alertas' (cambiando su estado a DESACTIVADA)."
    )
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

    @Hidden
    ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @Parameter(name = "id", description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String id,
            @Parameter(name = "alertId", description = "ID de la alerta a desactivar", example = "501") Long alertId
    );

    @Operation(
            summary = "Listar notificaciones de la campana",
            description = "Recupera el listado de notificaciones para el desplegable de la campana (con el enlace inferior hacia 'Mis búsquedas y alertas'). Sin parámetros de paginación devuelve el arreglo directo; con 'page', 'size', 'read' o 'unreadOnly' devuelve resultado paginado."
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
            @Parameter(description = "Filtrar solo notificaciones no leídas", example = "false") Boolean unreadOnly,
            @Parameter(hidden = true) Boolean soloNoLeidas,
            @Parameter(description = "Filtrar por estado de lectura (true=leídas, false=no leídas)", example = "false") Boolean read,
            @Parameter(hidden = true) Boolean leida,
            @Parameter(description = "Número de página (iniciando en 0, >= 0)", example = "0") @Min(value = 0, message = "page debe ser 0 o mayor") Integer page,
            @Parameter(description = "Tamaño de página (1 a 100)", example = "20") @Min(value = 1, message = "size debe ser al menos 1") @Max(value = 100, message = "size no puede superar 100") Integer size
    );

    @Operation(summary = "Marcar notificación individual como leída", description = "Actualiza el estado de la notificación a leída (read=true) cuando el usuario la selecciona en la campana")
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

    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Marca en lote todas las notificaciones pendientes del usuario como leídas y deja el contador de la burbuja morada en 0")
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

    @Operation(
            summary = "Obtener conteo de notificaciones sin leer (burbuja morada de la campana)",
            description = "Devuelve el número de notificaciones sin leer para renderizar la burbuja morada con número blanco sobre el icono de la campana en el frontend sin descargar toda la lista."
    )
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

    @Operation(
            summary = "Suscribirse al flujo de notificaciones en tiempo real (SSE)",
            description = "Abre un flujo Server-Sent Events para que la campana sea reactiva y actualice en tiempo real las notificaciones sin leer (siguiendo la gama de colores morada/blanca de StyleRadar) sin recargar la página (F5)."
    )
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
