package edu.dosw.styleradar.notificaciones.domain.exception;

public class ServicioExternoNoDisponibleException extends RuntimeException {

    private final CodigoError codigo;

    public ServicioExternoNoDisponibleException(String message) {
        this(message, CodigoError.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    public ServicioExternoNoDisponibleException(String message, CodigoError codigo) {
        super(message);
        this.codigo = codigo;
    }

    public ServicioExternoNoDisponibleException(String message, Throwable cause) {
        this(message, cause, CodigoError.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    public ServicioExternoNoDisponibleException(String message, Throwable cause, CodigoError codigo) {
        super(message, cause);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
