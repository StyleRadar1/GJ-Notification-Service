package edu.dosw.styleradar.notificaciones.domain.port.out;

public interface CorreoPort {

    /**
     * Pide enviar un correo. El envio no bloquea a quien llama y no lanza excepciones: si el servidor de
     * correo falla, el adaptador reintenta y, agotados los intentos, guarda el correo como pendiente para
     * volver a intentarlo, incluso despues de un reinicio del servicio.
     */
    void enviar(Long usuarioId, String destinatario, String asunto, String cuerpo);
}
