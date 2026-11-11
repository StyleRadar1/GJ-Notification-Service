package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

public class Alerta {

    private Long id;
    private Long usuarioId;
    private TipoAlerta tipoAlerta;
    private Long itemId;
    private String tallaObjetivo;
    private Long busquedaGuardadaId;
    private CanalNotificacion canal;
    private EstadoAlerta estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Alerta() {
    }

    public Alerta(
            Long id,
            Long usuarioId,
            TipoAlerta tipoAlerta,
            Long itemId,
            String tallaObjetivo,
            Long busquedaGuardadaId,
            CanalNotificacion canal,
            EstadoAlerta estado,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipoAlerta = tipoAlerta;
        this.itemId = itemId;
        this.tallaObjetivo = tallaObjetivo;
        this.busquedaGuardadaId = busquedaGuardadaId;
        this.canal = canal != null ? canal : CanalNotificacion.IN_APP;
        this.estado = estado != null ? estado : EstadoAlerta.ACTIVA;
        this.fechaCreacion = fechaCreacion != null ? fechaCreacion : LocalDateTime.now();
        this.fechaActualizacion = fechaActualizacion != null ? fechaActualizacion : LocalDateTime.now();
    }

    public boolean estaActiva() {
        return EstadoAlerta.ACTIVA.equals(this.estado);
    }

    public void desactivar() {
        this.estado = EstadoAlerta.DESACTIVADA;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void marcarDisparada() {
        this.estado = EstadoAlerta.DISPARADA;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public TipoAlerta getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(TipoAlerta tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getTallaObjetivo() {
        return tallaObjetivo;
    }

    public void setTallaObjetivo(String tallaObjetivo) {
        this.tallaObjetivo = tallaObjetivo;
    }

    public Long getBusquedaGuardadaId() {
        return busquedaGuardadaId;
    }

    public void setBusquedaGuardadaId(Long busquedaGuardadaId) {
        this.busquedaGuardadaId = busquedaGuardadaId;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public void setCanal(CanalNotificacion canal) {
        this.canal = canal;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public void setEstado(EstadoAlerta estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
