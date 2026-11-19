package edu.dosw.styleradar.notificaciones.domain.exception;

public class ConflictoException extends RuntimeException {

    private final CodigoError codigo;

    public ConflictoException(String message) {
        this(message, CodigoError.CONFLICT);
    }

    public ConflictoException(String message, CodigoError codigo) {
        super(message);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
