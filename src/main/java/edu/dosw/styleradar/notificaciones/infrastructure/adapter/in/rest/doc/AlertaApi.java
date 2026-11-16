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
        description = "Endpoints bajo /api/v1/usuarios/{usuarioId} para gestionar alertas de disponibilidad, busquedas guardadas y notificaciones en tiempo real (con validación de autenticación)"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(
            summary = "Crear nueva alerta",
            description = "Configura una alerta para un item agotado o una busqueda guardada del usuario indicado (validado contra el token JWT)"
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
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"ACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T10:15:30\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada invalidos",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ValidacionFallida",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Error de validacion en la solicitud\",\"status\":400,\"detail\":\"El itemId debe ser un numero positivo; El tipo de alerta es obligatorio\",\"instance\":\"/api/v1/usuarios/101/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT expirado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "NoAutenticado",
                                    value = "{\"type\":\"about:blank\",\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente, invalido o expirado\",\"instance\":\"/api/v1/usuarios/101/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: el ID del path no coincide con el token JWT del usuario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AccesoDenegado",
                                    value = "{\"type\":\"https://styleradar.co/errors/403\",\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion para acceder o modificar los recursos del usuario 202\",\"instance\":\"/api/v1/usuarios/202/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una alerta activa para este recurso",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "AlertaDuplicada",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Conflicto de estado del recurso\",\"status\":409,\"detail\":\"Ya tienes una alerta activa para este item/talla\",\"instance\":\"/api/v1/usuarios/101/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Regla de negocio no satisfecha (ej. item con stock disponible)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ItemConStock",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Regla de negocio no satisfecha\",\"status\":422,\"detail\":\"No se puede crear alerta para un item que cuenta con stock disponible\",\"instance\":\"/api/v1/usuarios/101/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Microservicio de Catalogo no disponible",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "CatalogoCaido",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Servicio no disponible\",\"status\":503,\"detail\":\"El servicio de catalogo no responde\",\"instance\":\"/api/v1/usuarios/101/alertas\"}"
                            )
                    )
            )
    })
    ResponseEntity<AlertaResponse> crearAlerta(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Valid CrearAlertaRequest request
    );

    @Operation(summary = "Listar mis alertas activas", description = "Retorna las alertas de stock y busquedas guardadas asociadas al usuario")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de alertas obtenido",
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
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: el ID del path no coincide con el token JWT del usuario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion para acceder o modificar los recursos de otro usuario\"}")
                    )
            )
    })
    ResponseEntity<List<AlertaResponse>> listarMisAlertas(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId
    );

    @Operation(summary = "Desactivar alerta por ID (DELETE)", description = "Cambia el estado de la alerta a DESACTIVADA")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class),
                            examples = @ExampleObject(
                                    name = "AlertaDesactivada",
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"DESACTIVADA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T12:00:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion para modificar recursos de otro usuario\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Recurso no encontrado\",\"status\":404,\"detail\":\"No se encontro la alerta con ID: 999\"}")
                    )
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlerta(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Parameter(description = "ID de la alerta a desactivar", example = "501") Long alertaId
    );

    @Operation(summary = "Desactivar alerta por ID (PATCH)", description = "Alias PATCH para desactivar alerta")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta no encontrada",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Parameter(description = "ID de la alerta a desactivar", example = "501") Long alertaId
    );

    @Operation(
            summary = "Listar notificaciones del usuario",
            description = "Consulta la bandeja de notificaciones. Si no se especifican parametros de paginacion ('page', 'size') "
                    + "ni de estado ('leida'), devuelve un array JSON directo con todas las notificaciones. "
                    + "Si se pasa 'page', 'size' o 'leida', la respuesta cambia a una estructura paginada "
                    + "({ content: [...], page: 0, size: 20, totalElements: N, totalPages: M, first: true, last: false }). "
                    + "Se puede filtrar por solo no leidas con 'soloNoLeidas=true' o 'leida=false'; 'leida' tiene precedencia."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de notificaciones (array directo o estructura paginada segun parametros)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "ArrayDirectoCompatibilidad",
                                            summary = "Sin parametros de paginacion (formato clasico)",
                                            value = "[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}]"
                                    ),
                                    @ExampleObject(
                                            name = "EstructuraPaginada",
                                            summary = "Con page, size o leida (formato paginado GJ-118)",
                                            value = "{\"content\":[{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":false,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}],\"page\":0,\"size\":20,\"totalElements\":1,\"totalPages\":1,\"first\":true,\"last\":true}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parametros de paginacion invalidos (page < 0, size < 1 o size > 100)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ParametrosInvalidos",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Error de validacion en la solicitud\",\"status\":400,\"detail\":\"El tamano maximo de pagina es 100\",\"instance\":\"/api/v1/usuarios/101/notificaciones\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion para consultar notificaciones de otro usuario\"}")
                    )
            )
    })
    ResponseEntity<?> listarMisNotificaciones(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Parameter(description = "Filtrar unicamente notificaciones no leidas (equivale a leida=false)", example = "false") Boolean soloNoLeidas,
            @Parameter(description = "Filtrar por leidas (true) o no leidas (false); si se envia, tiene precedencia sobre soloNoLeidas", example = "false") Boolean leida,
            @Parameter(description = "Numero de pagina (cero-indexado, >= 0)", example = "0") @Min(value = 0, message = "page debe ser 0 o mayor") Integer page,
            @Parameter(description = "Tamano de pagina (1 a 100)", example = "20") @Min(value = 1, message = "size debe ser al menos 1") @Max(value = 100, message = "size no puede superar 100") Integer size
    );

    @Operation(summary = "Marcar notificacion como leida", description = "Actualiza el estado de una notificacion especifica a leida=true")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificacion marcada como leida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NotificacionResponse.class),
                            examples = @ExampleObject(
                                    name = "NotificacionLeida",
                                    value = "{\"id\":901,\"usuarioId\":101,\"tipo\":\"REPOSICION_STOCK\",\"titulo\":\"Talla disponible\",\"mensaje\":\"El item 1045 volvio a tener stock en talla M\",\"leida\":true,\"fechaCreacion\":\"2026-11-06T08:00:00\",\"referenciaId\":\"1045\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion para modificar notificaciones de otro usuario\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notificacion no encontrada",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Recurso no encontrado\",\"status\":404,\"detail\":\"No se encontro la notificacion con ID: 999\"}")
                    )
            )
    })
    ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Parameter(description = "ID de la notificacion", example = "901") Long notificacionId
    );

    @Operation(summary = "Marcar todas las notificaciones como leidas", description = "Marca todas las notificaciones pendientes del usuario como leidas")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cantidad de notificaciones actualizadas a estado leido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContadorNoLeidasResponse.class),
                            examples = @ExampleObject(
                                    name = "TodasLeidas",
                                    value = "{\"usuarioId\":101,\"cantidadNoLeidas\":0}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion\"}")
                    )
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId
    );

    @Operation(summary = "Obtener contador de no leidas", description = "Retorna el numero de notificaciones sin leer para la campana del frontend")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Contador obtenido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ContadorNoLeidasResponse.class),
                            examples = @ExampleObject(
                                    name = "ContadorCampana",
                                    value = "{\"usuarioId\":101,\"cantidadNoLeidas\":3}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion\"}")
                    )
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId
    );

    @Operation(summary = "Suscribirse a campana en tiempo real (SSE)", description = "Abre un flujo Server-Sent Events para recibir notificaciones sin recargar")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flujo SSE establecido exitosamente",
                    content = @Content(mediaType = "text/event-stream")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente o invalido\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes autorizacion\"}")
                    )
            )
    })
    SseEmitter suscribirCampanaTiempoReal(
            @Parameter(description = "ID del usuario propietario del recurso (o alias 'me')", example = "101") String usuarioId,
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = "Last-Event-ID",
                    description = "Id del ultimo evento recibido. El navegador lo envia solo al reconectarse y el servicio "
                            + "reenvia las notificaciones posteriores (hasta 200, las mas recientes).",
                    required = false,
                    example = "42"
            ) String ultimoEventoId
    );
}
