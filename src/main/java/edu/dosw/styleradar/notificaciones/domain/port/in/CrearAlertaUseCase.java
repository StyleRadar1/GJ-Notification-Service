package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;

public interface CrearAlertaUseCase {
    Alerta crearAlerta(Alerta alerta);
}
