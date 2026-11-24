package edu.dosw.styleradar.notificaciones.domain.port.in;

/** Borra los datos de notificaciones de un usuario cuya cuenta fue eliminada en ms-Users. */
public interface EliminarDatosUsuarioUseCase {

    /**
     * Elimina sus notificaciones, alertas, preferencias, correos pendientes y busquedas guardadas, cierra sus
     * conexiones en tiempo real y deja su cuenta marcada como eliminada (sin correo) para que un evento que
     * llegue tarde no le genere avisos. Es idempotente: repetirlo no falla.
     */
    void eliminarDatosDeUsuario(Long usuarioId);
}
