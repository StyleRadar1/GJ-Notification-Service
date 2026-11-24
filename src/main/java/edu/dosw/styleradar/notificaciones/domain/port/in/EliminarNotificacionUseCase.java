package edu.dosw.styleradar.notificaciones.domain.port.in;

/** Eliminar notificaciones de la bandeja del usuario (borrado logico). */
public interface EliminarNotificacionUseCase {

    /**
     * Elimina una notificacion del usuario. Falla con "no encontrada" si no existe o ya fue eliminada, y con
     * acceso denegado si pertenece a otro usuario.
     */
    void eliminar(Long notificacionId, Long usuarioId);

    /** Elimina todas las notificaciones leidas del usuario y devuelve cuantas fueron. */
    int eliminarLeidas(Long usuarioId);
}
