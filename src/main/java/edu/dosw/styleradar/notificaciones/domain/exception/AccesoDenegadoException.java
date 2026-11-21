package edu.dosw.styleradar.notificaciones.domain.exception;

public class AccesoDenegadoException extends RuntimeException {

    private final CodigoError codigo;

    public AccesoDenegadoException(String message) {
        this(message, CodigoError.ACCESS_DENIED);
    }

    public AccesoDenegadoException(String message, CodigoError codigo) {
        super(message);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
