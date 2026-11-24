package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;

public interface RealTimeNotificationPort {

    void emitirNotificacionTiempoReal(Long usuarioId, Notificacion notificacion);

    /** Cierra las conexiones abiertas del usuario (cuenta eliminada). Por defecto no hace nada. */
    default void cerrarConexionesDeUsuario(Long usuarioId) {
    }
}
