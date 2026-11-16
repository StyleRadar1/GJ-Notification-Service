package edu.dosw.styleradar.notificaciones.domain.port.out;

/** Envio de un correo en el momento, para reintentar los pendientes y saber si fallaron. */
public interface EnvioCorreoPort {

    /** Hay un servidor de correo configurado. Sin el, reintentar no tiene sentido. */
    boolean estaDisponible();

    /**
     * Envia el correo y espera el resultado.
     *
     * @throws edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException si el
     *         servidor de correo no lo acepta
     */
    void enviarSincrono(String destinatario, String asunto, String cuerpo);
}
