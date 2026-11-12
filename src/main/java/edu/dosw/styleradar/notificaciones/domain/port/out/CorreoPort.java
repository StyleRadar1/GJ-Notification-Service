package edu.dosw.styleradar.notificaciones.domain.port.out;

public interface CorreoPort {

    /**
     * Pide enviar un correo. El envio no bloquea a quien llama y no lanza excepciones: si el servidor de
     * correo falla, el adaptador reintenta y, agotados los intentos, lo registra en el log.
     */
    void enviar(String destinatario, String asunto, String cuerpo);
}
