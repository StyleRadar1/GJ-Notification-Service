package edu.dosw.styleradar.notificaciones.domain.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    private final CodigoError codigo;

    public RecursoNoEncontradoException(String message) {
        this(message, CodigoError.NOT_FOUND);
    }

    public RecursoNoEncontradoException(String message, CodigoError codigo) {
        super(message);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
