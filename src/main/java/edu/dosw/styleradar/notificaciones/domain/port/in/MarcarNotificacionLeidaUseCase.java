package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;

public interface MarcarNotificacionLeidaUseCase {
    Notificacion marcarComoLeida(Long notificacionId, Long usuarioId);
    int marcarTodasComoLeidas(Long usuarioId);
}
