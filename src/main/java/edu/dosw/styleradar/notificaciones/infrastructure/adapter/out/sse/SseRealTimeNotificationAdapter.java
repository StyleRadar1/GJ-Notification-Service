package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.sse;

import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.application.mapper.in.NotificacionDtoMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseRealTimeNotificationAdapter implements RealTimeNotificationPort {

    private static final long DEFAULT_SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final NotificacionDtoMapper notificacionDtoMapper;

    public SseEmitter registrarSuscripcion(Long usuarioId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT_MS);
        List<SseEmitter> userEmitters = emittersByUser.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> removerEmitter(usuarioId, emitter));
        emitter.onTimeout(() -> removerEmitter(usuarioId, emitter));
        emitter.onError(ex -> removerEmitter(usuarioId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Suscripcion SSE activa"));
        } catch (IOException ex) {
            removerEmitter(usuarioId, emitter);
        }

        return emitter;
    }

    @Override
    public void emitirNotificacionTiempoReal(Long usuarioId, Notificacion notificacion) {
        if (usuarioId == null || notificacion == null) {
            return;
        }
        List<SseEmitter> userEmitters = emittersByUser.get(usuarioId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        NotificacionResponse payload = notificacionDtoMapper.toResponse(notificacion);
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notificacion").data(payload));
            } catch (Exception ex) {
                log.debug("Cerrando emitter SSE inactivo para el usuario {}", usuarioId);
                removerEmitter(usuarioId, emitter);
            }
        }
    }

    public int contarSuscripcionesActivas(Long usuarioId) {
        List<SseEmitter> list = emittersByUser.get(usuarioId);
        return list != null ? list.size() : 0;
    }

    private void removerEmitter(Long usuarioId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByUser.get(usuarioId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByUser.remove(usuarioId);
            }
        }
    }
}
