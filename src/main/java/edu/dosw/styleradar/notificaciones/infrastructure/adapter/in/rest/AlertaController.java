package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.ContadorNoLeidasResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.application.dto.response.PaginaResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}")
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

    private static final int PAGINA_POR_DEFECTO = 0;
    private static final int TAMANO_POR_DEFECTO = 20;

    @Override
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/alertas")
    public ResponseEntity<AlertaResponse> crearAlerta(
            @PathVariable("usuarioId") String usuarioId,
            @Valid @RequestBody CrearAlertaRequest request
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        Alerta dominio = alertaDtoMapper.toDomain(request, targetId);
        Alerta creada = crearAlertaUseCase.crearAlerta(dominio);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertaDtoMapper.toResponse(creada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaResponse>> listarMisAlertas(
            @PathVariable("usuarioId") String usuarioId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        List<AlertaResponse> respuestas = listarAlertasUseCase.listarAlertasPorUsuario(targetId)
                .stream()
                .map(alertaDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/alertas/{alertaId}")
    public ResponseEntity<AlertaResponse> desactivarAlerta(
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("alertaId") Long alertaId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        Alerta desactivada = desactivarAlertaUseCase.desactivarAlerta(alertaId, targetId);
        return ResponseEntity.ok(alertaDtoMapper.toResponse(desactivada));
    }

    @Override
    @PatchMapping("/alertas/{alertaId}/desactivar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertaResponse> desactivarAlertaPatch(
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("alertaId") Long alertaId
    ) {
        return desactivarAlerta(usuarioId, alertaId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notificaciones")
    public ResponseEntity<?> listarMisNotificaciones(
            @PathVariable("usuarioId") String usuarioId,
            @RequestParam(value = "soloNoLeidas", required = false) Boolean soloNoLeidas,
            @RequestParam(value = "leida", required = false) Boolean leida,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        boolean paginada = page != null || size != null || leida != null;
        if (!paginada) {
            List<NotificacionResponse> respuestas = listarNotificacionesUseCase
                    .listarNotificacionesPorUsuario(targetId, soloNoLeidas)
                    .stream()
                    .map(notificacionDtoMapper::toResponse)
                    .toList();
            return ResponseEntity.ok(respuestas);
        }
        Boolean filtroLeida = leida != null ? leida : (Boolean.TRUE.equals(soloNoLeidas) ? Boolean.FALSE : null);
        var pagina = listarNotificacionesUseCase.listarNotificacionesPaginadas(
                targetId,
                filtroLeida,
                page != null ? page : PAGINA_POR_DEFECTO,
                size != null ? size : TAMANO_POR_DEFECTO);
        return ResponseEntity.ok(PaginaResponse.de(pagina, notificacionDtoMapper::toResponse));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/notificaciones/{notificacionId}/leer")
    public ResponseEntity<NotificacionResponse> marcarNotificacionComoLeida(
            @PathVariable("usuarioId") String usuarioId,
            @PathVariable("notificacionId") Long notificacionId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        Notificacion actualizada = marcarNotificacionLeidaUseCase.marcarComoLeida(notificacionId, targetId);
        return ResponseEntity.ok(notificacionDtoMapper.toResponse(actualizada));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/notificaciones/leer-todas")
    public ResponseEntity<ContadorNoLeidasResponse> marcarTodasComoLeidas(
            @PathVariable("usuarioId") String usuarioId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        marcarNotificacionLeidaUseCase.marcarTodasComoLeidas(targetId);
        long restantes = listarNotificacionesUseCase.contarNoLeidas(targetId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(targetId, restantes));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notificaciones/no-leidas/contador")
    public ResponseEntity<ContadorNoLeidasResponse> contarNoLeidas(
            @PathVariable("usuarioId") String usuarioId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
        long cantidad = listarNotificacionesUseCase.contarNoLeidas(targetId);
        return ResponseEntity.ok(new ContadorNoLeidasResponse(targetId, cantidad));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/notificaciones/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirCampanaTiempoReal(
            @PathVariable("usuarioId") String usuarioId,
            @RequestHeader(value = "Last-Event-ID", required = false) String ultimoEventoId
    ) {
        Long targetId = userExtractor.validarAccesoUsuario(usuarioId);
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
