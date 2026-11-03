package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.AlertaApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DesactivarAlertaUseCaseImpl implements DesactivarAlertaUseCase {

    private final AlertaApplicationService alertaApplicationService;

    public DesactivarAlertaUseCaseImpl(
            AlertaRepositoryPort alertaRepositoryPort,
            AlertaValidator alertaValidator
    ) {
        this(new AlertaApplicationServiceImpl(alertaRepositoryPort, null, null, null, null, alertaValidator));
    }

    @Override
    public Alerta desactivarAlerta(Long alertaId, Long usuarioId) {
        return alertaApplicationService.desactivarAlerta(alertaId, usuarioId);
    }
}
