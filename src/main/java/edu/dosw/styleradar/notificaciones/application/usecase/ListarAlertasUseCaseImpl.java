package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.AlertaApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarAlertasUseCaseImpl implements ListarAlertasUseCase {

    private final AlertaApplicationService alertaApplicationService;

    public ListarAlertasUseCaseImpl(AlertaRepositoryPort alertaRepositoryPort) {
        this(new AlertaApplicationServiceImpl(alertaRepositoryPort, null, null, null, null, null));
    }

    @Override
    public List<Alerta> listarAlertasPorUsuario(Long usuarioId) {
        return alertaApplicationService.listarAlertasPorUsuario(usuarioId);
    }
}
