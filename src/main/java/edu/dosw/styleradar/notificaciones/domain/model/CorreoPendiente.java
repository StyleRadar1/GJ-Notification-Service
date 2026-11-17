package edu.dosw.styleradar.notificaciones.domain.model;

import java.time.LocalDateTime;

/** Un correo que no se pudo enviar y se vuelve a intentar hasta agotar sus intentos. */
public class CorreoPendiente {

    private static final int LARGO_MAXIMO_ERROR = 1000;

    private Long id;
    private final Long usuarioId;
    private final String destinatario;
    private final String asunto;
    private final String cuerpo;
    private int intentos;
    private final int maxIntentos;
    private EstadoCorreo estado;
    private String ultimoError;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public CorreoPendiente(
            Long id,
            Long usuarioId,
            String destinatario,
            String asunto,
            String cuerpo,
            int intentos,
            int maxIntentos,
            EstadoCorreo estado,
            String ultimoError,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.intentos = intentos;
        this.maxIntentos = maxIntentos;
        this.estado = estado;
        this.ultimoError = ultimoError;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    /** Un correo nuevo, sin intentos de recuperacion todavia. */
    public static CorreoPendiente nuevo(
            Long usuarioId, String destinatario, String asunto, String cuerpo, int maxIntentos, String error) {
        LocalDateTime ahora = LocalDateTime.now();
        return new CorreoPendiente(null, usuarioId, destinatario, asunto, cuerpo, 0, Math.max(1, maxIntentos),
                EstadoCorreo.PENDIENTE, recortar(error), ahora, ahora);
    }

    public boolean puedeReintentarse() {
        return estado == EstadoCorreo.PENDIENTE && intentos < maxIntentos;
    }

    public void marcarEnviado() {
        this.estado = EstadoCorreo.ENVIADO;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /** Cuenta un intento fallido; al llegar al maximo el correo queda FALLIDO y ya no se reintenta. */
    public void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = recortar(error);
        this.estado = intentos >= maxIntentos ? EstadoCorreo.FALLIDO : EstadoCorreo.PENDIENTE;
        this.fechaActualizacion = LocalDateTime.now();
    }

    private static String recortar(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > LARGO_MAXIMO_ERROR ? error.substring(0, LARGO_MAXIMO_ERROR) : error;
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

    public String getDestinatario() {
        return destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public int getIntentos() {
        return intentos;
    }

    public int getMaxIntentos() {
        return maxIntentos;
    }

    public EstadoCorreo getEstado() {
        return estado;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
