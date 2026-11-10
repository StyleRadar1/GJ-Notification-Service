package edu.dosw.styleradar.notificaciones.infrastructure.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registro de las conexiones SSE abiertas por usuario. Lo comparten el controlador (que abre la
 * suscripcion) y el adaptador de salida (que empuja las notificaciones), de modo que el controlador
 * no necesita conocer ningun adaptador de salida.
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long DEFAULT_SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter registrar(Long usuarioId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_SSE_TIMEOUT_MS);
        List<SseEmitter> userEmitters = emittersByUser.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> remover(usuarioId, emitter));
        emitter.onTimeout(() -> remover(usuarioId, emitter));
        emitter.onError(ex -> remover(usuarioId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Suscripcion SSE activa"));
        } catch (IOException ex) {
            remover(usuarioId, emitter);
        }
        return emitter;
    }

    /**
     * Envia un evento a todas las conexiones del usuario. El id del evento permite que el navegador, al
     * reconectarse, avise con la cabecera Last-Event-ID cual fue el ultimo que recibio.
     */
    public void enviar(Long usuarioId, String nombreEvento, String idEvento, Object datos) {
        List<SseEmitter> userEmitters = emittersByUser.get(usuarioId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().id(idEvento).name(nombreEvento).data(datos));
            } catch (Exception ex) {
                log.debug("Cerrando emitter SSE inactivo para el usuario {}", usuarioId);
                remover(usuarioId, emitter);
            }
        }
    }

    /** Envia un evento a una sola conexion; se usa para reenviar lo que el usuario perdio al reconectarse. */
    public void enviarA(SseEmitter emitter, String nombreEvento, String idEvento, Object datos) {
        try {
            emitter.send(SseEmitter.event().id(idEvento).name(nombreEvento).data(datos));
        } catch (IOException ex) {
            log.debug("No se pudo reenviar el evento {} a la conexion SSE", idEvento);
        }
    }

    public int contarSuscripcionesActivas(Long usuarioId) {
        List<SseEmitter> list = emittersByUser.get(usuarioId);
        return list != null ? list.size() : 0;
    }

    private void remover(Long usuarioId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByUser.get(usuarioId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByUser.remove(usuarioId);
            }
        }
    }
}
