package edu.dosw.styleradar.notificaciones.domain.model;

public enum CanalNotificacion {
    IN_APP,
    EMAIL,
    PUSH,
    /** Plataforma y correo: el aviso de la alerta tambien se envia por correo. */
    AMBOS;

    /** La alerta pidio correo al crearse, sin importar la preferencia por tipo de aviso. */
    public boolean incluyeCorreo() {
        return this == EMAIL || this == AMBOS;
    }
}
