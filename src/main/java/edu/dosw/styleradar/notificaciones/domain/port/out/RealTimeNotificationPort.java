package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;

public interface RealTimeNotificationPort {

    void emitirNotificacionTiempoReal(Long usuarioId, Notificacion notificacion);
}
