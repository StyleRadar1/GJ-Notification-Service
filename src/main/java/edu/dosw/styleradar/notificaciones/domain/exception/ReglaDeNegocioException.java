package edu.dosw.styleradar.notificaciones.domain.exception;

public class ReglaDeNegocioException extends RuntimeException {

    private final CodigoError codigo;

    public ReglaDeNegocioException(String message) {
        this(message, CodigoError.BUSINESS_RULE_VIOLATION);
    }

    public ReglaDeNegocioException(String message, CodigoError codigo) {
        super(message);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
