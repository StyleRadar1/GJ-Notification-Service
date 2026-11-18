package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ActivarAlertaUseCase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActivarAlertaUseCaseImpl implements ActivarAlertaUseCase {

    private final AlertaApplicationService alertaApplicationService;

    @Override
    public Alerta activarAlerta(Long alertaId, Long usuarioId) {
        return alertaApplicationService.activarAlerta(alertaId, usuarioId);
    }
}
