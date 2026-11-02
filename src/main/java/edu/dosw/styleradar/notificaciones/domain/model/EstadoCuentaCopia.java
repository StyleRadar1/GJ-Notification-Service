package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

public class EstadoCuentaCopia {

    private Long usuarioId;
    private String email;
    private EstadoCuenta estado;
    private LocalDateTime fechaActualizacion;

    public EstadoCuentaCopia() {
    }

    public EstadoCuentaCopia(Long usuarioId, String email, EstadoCuenta estado, LocalDateTime fechaActualizacion) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.estado = estado != null ? estado : EstadoCuenta.ACTIVO;
        this.fechaActualizacion = fechaActualizacion != null ? fechaActualizacion : LocalDateTime.now();
    }

    public boolean puedeRecibirNotificaciones() {
        return EstadoCuenta.ACTIVO.equals(this.estado);
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
