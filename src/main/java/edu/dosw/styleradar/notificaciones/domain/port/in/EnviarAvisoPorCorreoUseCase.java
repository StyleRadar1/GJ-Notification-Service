package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;

public interface EnviarAvisoPorCorreoUseCase {

    /** Envia el aviso por correo solo si el usuario lo pidio para ese tipo; nunca lanza excepciones. */
    void enviarSiCorresponde(Notificacion notificacion);
}
