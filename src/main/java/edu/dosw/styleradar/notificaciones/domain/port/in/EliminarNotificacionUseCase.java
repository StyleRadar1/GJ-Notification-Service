package edu.dosw.styleradar.notificaciones.domain.port.in;

/** Eliminar notificaciones de la bandeja del usuario (borrado logico). */
public interface EliminarNotificacionUseCase {

    /** Tope de notificaciones que se pueden eliminar en una sola solicitud. */
    int MAXIMO_POR_SOLICITUD = 100;

    /**
     * Elimina una notificacion del usuario. Falla con "no encontrada" si no existe o ya fue eliminada, y con
     * acceso denegado si pertenece a otro usuario.
     */
    void eliminar(Long notificacionId, Long usuarioId);

    /** Elimina todas las notificaciones leidas del usuario y devuelve cuantas fueron. */
    int eliminarLeidas(Long usuarioId);

    /**
     * Elimina las notificaciones indicadas (leidas o no) que sean del usuario y sigan visibles. Las que no existen,
     * son de otro usuario o ya fueron eliminadas se ignoran, para no revelar su existencia. Admite de 1 a
     * {@value #MAXIMO_POR_SOLICITUD} ids distintos y devuelve cuantas elimino realmente.
     */
    int eliminarSeleccionadas(Long usuarioId, java.util.Collection<Long> ids);
}
