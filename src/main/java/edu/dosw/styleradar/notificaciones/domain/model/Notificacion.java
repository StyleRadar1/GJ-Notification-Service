package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

public class Notificacion {

    private Long id;
    private Long usuarioId;
    private Long alertaId;
    private Long itemId;
    private Integer cicloReposicion;
    private TipoNotificacion tipo;
    private CanalNotificacion canal;
    private String titulo;
    private String mensaje;
    private String enlace;
    private boolean leida;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLectura;
    /** Evento externo que origino la notificacion; permite descartar entregas repetidas. Puede ser nulo. */
    private String eventoId;

    public Notificacion() {
    }

    public Notificacion(
            Long id,
            Long usuarioId,
            Long alertaId,
            Long itemId,
            Integer cicloReposicion,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje,
            String enlace,
            boolean leida,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaLectura
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.alertaId = alertaId;
        this.itemId = itemId;
        this.cicloReposicion = cicloReposicion;
        this.tipo = tipo != null ? tipo : TipoNotificacion.SISTEMA;
        this.canal = canal != null ? canal : CanalNotificacion.IN_APP;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.enlace = enlace;
        this.leida = leida;
        this.fechaCreacion = fechaCreacion != null ? fechaCreacion : LocalDateTime.now();
        this.fechaLectura = fechaLectura;
    }

    public void marcarComoLeida() {
        if (!this.leida) {
            this.leida = true;
            this.fechaLectura = LocalDateTime.now();
        }
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

    public Long getAlertaId() {
        return alertaId;
    }

    public void setAlertaId(Long alertaId) {
        this.alertaId = alertaId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getCicloReposicion() {
        return cicloReposicion;
    }

    public void setCicloReposicion(Integer cicloReposicion) {
        this.cicloReposicion = cicloReposicion;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public void setCanal(CanalNotificacion canal) {
        this.canal = canal;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEnlace() {
        return enlace;
    }

    public void setEnlace(String enlace) {
        this.enlace = enlace;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(LocalDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }
}
