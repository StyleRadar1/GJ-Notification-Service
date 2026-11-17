package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.PaginaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.TicketStreamResponse;
import edu.dosw.styleradar.notificaciones.application.mapper.in.AlertaDtoMapper;
import edu.dosw.styleradar.notificaciones.application.mapper.in.NotificacionDtoMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ActivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.CrearAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc.AlertaApi;
import edu.dosw.styleradar.notificaciones.infrastructure.security.AuthenticatedUserPrincipal;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import edu.dosw.styleradar.notificaciones.infrastructure.security.SecurityContextUserExtractor;
import edu.dosw.styleradar.notificaciones.infrastructure.sse.SseEmitterRegistry;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping({"/users/{id}", "/api/v1/users/{id}", "/api/v1/usuarios/{id}"})
@RequiredArgsConstructor
public class AlertaController implements AlertaApi {

    private final CrearAlertaUseCase crearAlertaUseCase;
    private final ListarAlertasUseCase listarAlertasUseCase;
    private final DesactivarAlertaUseCase desactivarAlertaUseCase;
    private final ActivarAlertaUseCase activarAlertaUseCase;
    private final ListarNotificacionesUseCase listarNotificacionesUseCase;
    private final MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase;
    private final AlertaDtoMapper alertaDtoMapper;
    private final NotificacionDtoMapper notificacionDtoMapper;
    private final SecurityContextUserExtractor userExtractor;
    private final SseEmitterRegistry sseRegistry;
    private final JwtTokenService jwtTokenService;

    private static final int PAGINA_POR_DEFECTO = 0;
    private static final int TAMANO_POR_DEFECTO = 20;

    @Override
    @PreAuthorize("isAuthenticated()")
    @PostMapping({"/alerts", "/alertas"})
    public ResponseEntity<AlertaResponse> crearAlerta(
            @PathVariable("id") String id,
            @Valid @RequestBody CrearAlertaRequest request
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        Alerta dominio = alertaDtoMapper.toDomain(request, targetId);
        Alerta creada = crearAlertaUseCase.crearAlerta(dominio);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaDtoMapper.toResponse(creada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/alerts", "/alertas"})
    public ResponseEntity<List<AlertaResponse>> listarMisAlertas(
            @PathVariable("id") String id
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        List<AlertaResponse> respuestas = listarAlertasUseCase.listarAlertasPorUsuario(targetId)
                .stream()
                .map(alertaDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping({"/alerts/{alertId}", "/alertas/{alertId}"})
    public ResponseEntity<AlertaResponse> desactivarAlerta(
            @PathVariable("id") String id,
            @PathVariable("alertId") Long alertId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        Alerta desactivada = desactivarAlertaUseCase.desactivarAlerta(alertId, targetId);
        return ResponseEntity.ok(alertaDtoMapper.toResponse(desactivada));
    }

    @Override
    @PatchMapping({"/alerts/{alertId}/deactivate", "/alertas/{alertId}/desactivar"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @PathVariable("id") String id,
            @PathVariable("alertId") Long alertId
    ) {
        return desactivarAlerta(id, alertId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping({"/alerts/{alertId}/activate", "/alertas/{alertId}/activar"})
    public ResponseEntity<AlertaResponse> activarAlerta(
            @PathVariable("id") String id,
            @PathVariable("alertId") Long alertId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        Alerta activada = activarAlertaUseCase.activarAlerta(alertId, targetId);
        return ResponseEntity.ok(alertaDtoMapper.toResponse(activada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/notifications", "/notificaciones"})
    public ResponseEntity<?> listarMisNotificaciones(
            @PathVariable("id") String id,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "soloNoLeidas", required = false) Boolean soloNoLeidas,
            @RequestParam(value = "read", required = false) Boolean read,
            @RequestParam(value = "leida", required = false) Boolean leida,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        Boolean soloNoLeidasEfectivo = unreadOnly != null ? unreadOnly : soloNoLeidas;
        Boolean leidaEfectivo = read != null ? read : leida;
        boolean paginada = page != null || size != null || leidaEfectivo != null;
        if (!paginada) {
            List<NotificacionResponse> respuestas = listarNotificacionesUseCase
                    .listarNotificacionesPorUsuario(targetId, soloNoLeidasEfectivo)
                    .stream()
                    .map(notificacionDtoMapper::toResponse)
                    .toList();
            return ResponseEntity.ok(respuestas);
        }
        Boolean filtroLeida = leidaEfectivo != null ? leidaEfectivo : (Boolean.TRUE.equals(soloNoLeidasEfectivo) ? Boolean.FALSE : null);
        var pagina = listarNotificacionesUseCase.listarNotificacionesPaginadas(
                targetId,
                filtroLeida,
                page != null ? page : PAGINA_POR_DEFECTO,
                size != null ? size : TAMANO_POR_DEFECTO);
        return ResponseEntity.ok(PaginaResponse.de(pagina, notificacionDtoMapper::toResponse));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping({"/notifications/{notificationId}/read", "/notificaciones/{notificationId}/leer"})
    public ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @PathVariable("id") String id,
            @PathVariable("notificationId") Long notificationId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        Notificacion actualizada = marcarNotificacionLeidaUseCase.marcarComoLeida(notificationId, targetId);
        return ResponseEntity.ok(notificacionDtoMapper.toResponse(actualizada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping({"/notifications/read-all", "/notificaciones/leer-todas"})
    public ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas(
            @PathVariable("id") String id
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        marcarNotificacionLeidaUseCase.marcarTodasComoLeidas(targetId);
        long restantes = listarNotificacionesUseCase.contarNoLeidas(targetId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(targetId, restantes));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping({"/notifications/unread/count", "/notifications/unread-count", "/notificaciones/no-leidas/contador", "/notificaciones/no-leidas/conteo"})
    public ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(
            @PathVariable("id") String id
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        long cantidad = listarNotificacionesUseCase.contarNoLeidas(targetId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(targetId, cantidad));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = {"/notifications/stream", "/notificaciones/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirCampanaTiempoReal(
            @PathVariable("id") String id,
            @RequestHeader(value = "Last-Event-ID", required = false) String ultimoEventoId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(id);
        SseEmitter emitter = sseRegistry.registrar(targetId);
        reenviarNotificacionesPerdidas(targetId, ultimoEventoId, emitter);
        return emitter;
    }

    private void reenviarNotificacionesPerdidas(Long usuarioId, String ultimoEventoId, SseEmitter emitter) {
        Long ultimoId = interpretarIdDeEvento(ultimoEventoId);
        if (ultimoId == null) {
            return;
        }
        listarNotificacionesUseCase.listarPosterioresA(usuarioId, ultimoId).forEach(notificacion ->
                sseRegistry.enviarA(emitter, "notificacion", String.valueOf(notificacion.getId()),
                        notificacionDtoMapper.toResponse(notificacion)));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PostMapping({"/notifications/stream-ticket", "/notificaciones/stream-ticket"})
    public ResponseEntity<TicketStreamResponse> emitirTicketStream(@PathVariable("id") String id) {
        userExtractor.validarAccesoUsuario(id);
        AuthenticatedUserPrincipal principal = userExtractor.obtenerPrincipal();
        String ticket = jwtTokenService.generarTicketStream(principal.usuarioId(), principal.email(), principal.roles());
        return ResponseEntity.ok(new TicketStreamResponse(ticket, jwtTokenService.ticketTtlSegundos()));
    }

    private Long interpretarIdDeEvento(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
