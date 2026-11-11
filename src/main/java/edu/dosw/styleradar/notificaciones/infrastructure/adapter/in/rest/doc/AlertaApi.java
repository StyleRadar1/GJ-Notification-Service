package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
        description = "Endpoints bajo /api/v1/usuarios/me para gestionar alertas de disponibilidad, busquedas guardadas y notificaciones en tiempo real"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(
            summary = "Crear nueva alerta",
            description = "Configura una alerta para un item agotado o una busqueda guardada del usuario autenticado"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Error de validacion en la solicitud\",\"status\":400,\"detail\":\"El itemId debe ser un numero positivo; El tipo de alerta es obligatorio\",\"instance\":\"/api/v1/usuarios/me/alertas\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"No autenticado\",\"status\":401,\"detail\":\"Token JWT ausente, invalido o expirado\",\"instance\":\"/api/v1/usuarios/me/alertas\"}"
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
                                    value = "{\"type\":\"about:blank\",\"title\":\"Conflicto de estado del recurso\",\"status\":409,\"detail\":\"Ya tienes una alerta activa para este item/talla\",\"instance\":\"/api/v1/usuarios/me/alertas\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violacion de regla de negocio (item con stock disponible o cuenta inactiva)",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "ItemConStock",
                                    value = "{\"type\":\"about:blank\",\"title\":\"Regla de negocio no satisfecha\",\"status\":422,\"detail\":\"No se puede crear alerta para un item que cuenta con stock disponible\",\"instance\":\"/api/v1/usuarios/me/alertas\"}"
                            )
                    )
            )
    })
    ResponseEntity<AlertaResponse> crearAlerta(@Valid CrearAlertaRequest request);

    @Operation(summary = "Listar mis alertas", description = "Retorna todas las alertas configuradas por el usuario autenticado")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de alertas obtenida",
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
            )
    })
    ResponseEntity<List<AlertaResponse>> listarMisAlertas();

    @Operation(summary = "Desactivar alerta", description = "Desactiva una alerta existente perteneciente al usuario autenticado")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class),
                            examples = @ExampleObject(
                                    name = "AlertaDesactivada",
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"INACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T12:00:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Intento de desactivar alerta de otro usuario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tiene permisos para modificar esta alerta\"}")
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
            @Parameter(description = "ID de la alerta a desactivar", example = "501") Long alertaId
    );

    @Operation(summary = "Desactivar alerta (Ruta alternativa PATCH)", description = "Desactiva una alerta existente perteneciente al usuario autenticado mediante metodo PATCH")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta desactivada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertaResponse.class),
                            examples = @ExampleObject(
                                    name = "AlertaDesactivadaPatch",
                                    value = "{\"id\":501,\"usuarioId\":101,\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":1045,\"tallaObjetivo\":\"M\",\"busquedaGuardadaId\":null,\"canal\":\"IN_APP\",\"estado\":\"INACTIVA\",\"fechaCreacion\":\"2026-11-02T10:15:30\",\"fechaActualizacion\":\"2026-11-02T12:00:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Intento de desactivar alerta de otro usuario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tiene permisos para modificar esta alerta\"}")
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
    ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @Parameter(description = "ID de la alerta a desactivar", example = "501") Long alertaId
    );

    @Operation(summary = "Listar mis notificaciones", description = "Retorna las notificaciones del usuario autenticado con filtro opcional de no leidas")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de notificaciones obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NotificacionResponse.class)),
                            examples = @ExampleObject(
                                    name = "ListaNotificaciones",
                                    value = "[{\"id\":901,\"usuarioId\":101,\"alertaId\":501,\"itemId\":1045,\"cicloReposicion\":1,\"tipo\":\"STOCK_REPUESTO\",\"canal\":\"IN_APP\",\"titulo\":\"Stock Disponible\",\"mensaje\":\"Chaqueta Denim Vintage vuelve a tener stock.\",\"enlace\":\"/prendas/1045\",\"leida\":false,\"fechaCreacion\":\"2026-11-02T12:00:00\",\"fechaLectura\":null}]"
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
            )
    })
    ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(
            @Parameter(description = "Filtrar unicamente notificaciones no leidas", example = "false") Boolean soloNoLeidas
    );

    @Operation(summary = "Marcar notificacion como leida", description = "Marca una notificacion especifica del usuario como leida")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificacion marcada como leida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NotificacionResponse.class),
                            examples = @ExampleObject(
                                    name = "NotificacionLeida",
                                    value = "{\"id\":901,\"usuarioId\":101,\"alertaId\":501,\"itemId\":1045,\"cicloReposicion\":1,\"tipo\":\"STOCK_REPUESTO\",\"canal\":\"IN_APP\",\"titulo\":\"Stock Disponible\",\"mensaje\":\"Chaqueta Denim Vintage vuelve a tener stock.\",\"enlace\":\"/prendas/1045\",\"leida\":true,\"fechaCreacion\":\"2026-11-02T12:00:00\",\"fechaLectura\":\"2026-11-02T12:05:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "La notificacion pertenece a otro usuario",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = "{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tiene permisos para modificar una notificacion de otro usuario\"}")
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
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas();

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
            )
    })
    ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas();

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
            )
    })
    SseEmitter suscribirCampanaTiempoReal();
}
