package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(
        name = "Alertas y Notificaciones",
        description = "Endpoints bajo /api/v1/usuarios/me para gestionar alertas de disponibilidad, busquedas guardadas y notificaciones en tiempo real"
)
@SecurityRequirement(name = "bearerAuth")
public interface AlertaApi {

    @Operation(summary = "Crear nueva alerta", description = "Configura una alerta para un item agotado o una busqueda guardada del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alerta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe una alerta activa para este recurso"),
            @ApiResponse(responseCode = "422", description = "Violacion de regla de negocio (ej. item con stock disponible o cuenta inactiva)")
    })
    ResponseEntity<AlertaResponse> crearAlerta(@Valid CrearAlertaRequest request);

    @Operation(summary = "Listar mis alertas", description = "Retorna todas las alertas configuradas por el usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Lista de alertas obtenida")
    ResponseEntity<List<AlertaResponse>> listarMisAlertas();

    @Operation(summary = "Desactivar alerta", description = "Desactiva una alerta existente perteneciente al usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerta desactivada"),
            @ApiResponse(responseCode = "403", description = "Intento de desactivar alerta de otro usuario"),
            @ApiResponse(responseCode = "404", description = "Alerta no encontrada")
    })
    ResponseEntity<AlertaResponse> desactivarAlerta(
            @Parameter(description = "ID de la alerta a desactivar") Long alertaId
    );

    @Operation(summary = "Listar mis notificaciones", description = "Retorna las notificaciones del usuario autenticado con filtro opcional de no leidas")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones obtenida")
    ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(
            @Parameter(description = "Filtrar unicamente notificaciones no leidas") Boolean soloNoLeidas
    );

    @Operation(summary = "Marcar notificacion como leida", description = "Marca una notificacion especifica del usuario como leida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacion marcada como leida"),
            @ApiResponse(responseCode = "403", description = "La notificacion pertenece a otro usuario"),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada")
    })
    ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @Parameter(description = "ID de la notificacion") Long notificacionId
    );

    @Operation(summary = "Marcar todas las notificaciones como leidas", description = "Marca todas las notificaciones pendientes del usuario como leidas")
    @ApiResponse(responseCode = "200", description = "Cantidad de notificaciones actualizadas")
    ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas();

    @Operation(summary = "Obtener contador de no leidas", description = "Retorna el numero de notificaciones sin leer para la campana del frontend")
    @ApiResponse(responseCode = "200", description = "Contador obtenido")
    ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas();

    @Operation(summary = "Suscribirse a campana en tiempo real (SSE)", description = "Abre un flujo Server-Sent Events para recibir notificaciones sin recargar")
    @ApiResponse(responseCode = "200", description = "Flujo SSE establecido")
    SseEmitter suscribirCampanaTiempoReal();
}
