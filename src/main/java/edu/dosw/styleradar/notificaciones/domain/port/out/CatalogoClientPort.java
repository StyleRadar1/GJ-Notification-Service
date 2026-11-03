package edu.dosw.styleradar.notificaciones.domain.port.out;

public interface CatalogoClientPort {

    boolean verificarItemAgotado(Long itemId, String tallaObjetivo);
}
