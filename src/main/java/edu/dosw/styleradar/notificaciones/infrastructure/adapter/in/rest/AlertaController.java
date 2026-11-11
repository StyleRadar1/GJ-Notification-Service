package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.application.mapper.in.AlertaDtoMapper;
import edu.dosw.styleradar.notificaciones.application.mapper.in.NotificacionDtoMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.CrearAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc.AlertaApi;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/usuarios/me")
@RequiredArgsConstructor
public class AlertaController implements AlertaApi {

    private final CrearAlertaUseCase crearAlertaUseCase;
    private final ListarAlertasUseCase listarAlertasUseCase;
    private final DesactivarAlertaUseCase desactivarAlertaUseCase;
    private final ListarNotificacionesUseCase listarNotificacionesUseCase;
    private final MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase;
    private final AlertaDtoMapper alertaDtoMapper;
    private final NotificacionDtoMapper notificacionDtoMapper;
    private final SecurityContextUserExtractor userExtractor;
    private final SseEmitterRegistry sseRegistry;

    @Override
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/alertas")
    public ResponseEntity<AlertaResponse> crearAlerta(@Valid @RequestBody CrearAlertaRequest request) {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        Alerta dominio = alertaDtoMapper.toDomain(request, usuarioId);
        Alerta creada = crearAlertaUseCase.crearAlerta(dominio);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaDtoMapper.toResponse(creada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaResponse>> listarMisAlertas() {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        List<AlertaResponse> respuestas = listarAlertasUseCase.listarAlertasPorUsuario(usuarioId)
                .stream()
                .map(alertaDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/alertas/{alertaId}")
    public ResponseEntity<AlertaResponse> desactivarAlerta(@PathVariable("alertaId") Long alertaId) {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        Alerta desactivada = desactivarAlertaUseCase.desactivarAlerta(alertaId, usuarioId);
        return ResponseEntity.ok(alertaDtoMapper.toResponse(desactivada));
    }

    @Override
    @PatchMapping("/alertas/{alertaId}/desactivar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertaResponse> desactivarAlertaPatch(@PathVariable("alertaId") Long alertaId) {
        return desactivarAlerta(alertaId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notificaciones")
    public ResponseEntity<List<NotificacionResponse>> listarMisNotificaciones(
            @RequestParam(value = "soloNoLeidas", required = false) Boolean soloNoLeidas
    ) {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        List<NotificacionResponse> respuestas = listarNotificacionesUseCase
                .listarNotificacionesPorUsuario(usuarioId, soloNoLeidas)
                .stream()
                .map(notificacionDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/notificaciones/{notificacionId}/leer")
    public ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @PathVariable("notificacionId") Long notificacionId
    ) {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        Notificacion actualizada = marcarNotificacionLeidaUseCase.marcarComoLeida(notificacionId, usuarioId);
        return ResponseEntity.ok(notificacionDtoMapper.toResponse(actualizada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/notificaciones/leer-todas")
    public ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas() {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        marcarNotificacionLeidaUseCase.marcarTodasComoLeidas(usuarioId);
        long restantes = listarNotificacionesUseCase.contarNoLeidas(usuarioId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(usuarioId, restantes));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notificaciones/no-leidas/contador")
    public ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas() {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        long cantidad = listarNotificacionesUseCase.contarNoLeidas(usuarioId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(usuarioId, cantidad));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirCampanaTiempoReal() {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        return sseRegistry.registrar(usuarioId);
    }
}
