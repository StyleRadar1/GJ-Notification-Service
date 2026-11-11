package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

/** Canal elegido por un usuario para un tipo de aviso. */
public class PreferenciaAviso {

    private final Long usuarioId;
    private final TipoNotificacion tipo;
    private CanalAviso canal;
    private LocalDateTime fechaActualizacion;

    public PreferenciaAviso(Long usuarioId, TipoNotificacion tipo, CanalAviso canal, LocalDateTime fechaActualizacion) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.canal = canal != null ? canal : CanalAviso.SOLO_PLATAFORMA;
        this.fechaActualizacion = fechaActualizacion != null ? fechaActualizacion : LocalDateTime.now();
    }

    /** Preferencia que se aplica cuando el usuario no ha guardado ninguna. */
    public static PreferenciaAviso porDefecto(Long usuarioId, TipoNotificacion tipo) {
        return new PreferenciaAviso(usuarioId, tipo, CanalAviso.SOLO_PLATAFORMA, null);
    }

    public boolean quiereCorreo() {
        return canal == CanalAviso.CORREO_Y_PLATAFORMA;
    }

    public void cambiarCanal(CanalAviso nuevoCanal) {
        this.canal = nuevoCanal;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public CanalAviso getCanal() {
        return canal;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
