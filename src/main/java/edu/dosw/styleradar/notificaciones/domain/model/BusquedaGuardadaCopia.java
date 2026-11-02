package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

public class BusquedaGuardadaCopia {

    private Long busquedaId;
    private Long usuarioId;
    private String nombre;
    private CriterioBusquedaVO criterios;
    private boolean activa;
    private LocalDateTime fechaSincronizacion;

    public BusquedaGuardadaCopia() {
    }

    public BusquedaGuardadaCopia(
            Long busquedaId,
            Long usuarioId,
            String nombre,
            CriterioBusquedaVO criterios,
            boolean activa,
            LocalDateTime fechaSincronizacion
    ) {
        this.busquedaId = busquedaId;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.criterios = criterios;
        this.activa = activa;
        this.fechaSincronizacion = fechaSincronizacion != null ? fechaSincronizacion : LocalDateTime.now();
    }

    public Long getBusquedaId() {
        return busquedaId;
    }

    public void setBusquedaId(Long busquedaId) {
        this.busquedaId = busquedaId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public CriterioBusquedaVO getCriterios() {
        return criterios;
    }

    public void setCriterios(CriterioBusquedaVO criterios) {
        this.criterios = criterios;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getFechaSincronizacion() {
        return fechaSincronizacion;
    }

    public void setFechaSincronizacion(LocalDateTime fechaSincronizacion) {
        this.fechaSincronizacion = fechaSincronizacion;
    }
}
