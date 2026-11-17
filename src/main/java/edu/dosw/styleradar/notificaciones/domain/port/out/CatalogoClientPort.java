package edu.dosw.styleradar.notificaciones.domain.port.out;

public interface CatalogoClientPort {

    boolean verificarItemAgotado(String itemId, String tallaObjetivo);
}
