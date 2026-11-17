package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;

public interface ActivarAlertaUseCase {
    Alerta activarAlerta(Long alertaId, Long usuarioId);
}
