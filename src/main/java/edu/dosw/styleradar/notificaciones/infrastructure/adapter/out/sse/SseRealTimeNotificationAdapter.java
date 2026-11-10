package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.sse;

import edu.dosw.styleradar.notificaciones.application.mapper.in.NotificacionDtoMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import edu.dosw.styleradar.notificaciones.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Empuja cada notificacion nueva a las conexiones SSE abiertas del usuario. */
@Component
@RequiredArgsConstructor
public class SseRealTimeNotificationAdapter implements RealTimeNotificationPort {

    private final SseEmitterRegistry registro;
    private final NotificacionDtoMapper notificacionDtoMapper;

    @Override
    public void emitirNotificacionTiempoReal(Long usuarioId, Notificacion notificacion) {
        if (usuarioId == null || notificacion == null) {
            return;
        }
        registro.enviar(usuarioId, "notificacion", notificacionDtoMapper.toResponse(notificacion));
    }
}
