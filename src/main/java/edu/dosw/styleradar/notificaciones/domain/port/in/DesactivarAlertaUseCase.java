package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;

public interface DesactivarAlertaUseCase {
    Alerta desactivarAlerta(Long alertaId, Long usuarioId);
}
