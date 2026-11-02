package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarAlertasUseCaseImpl implements ListarAlertasUseCase {

    private final AlertaRepositoryPort alertaRepositoryPort;

    @Override
    public List<Alerta> listarAlertasPorUsuario(Long usuarioId) {
        return alertaRepositoryPort.listarPorUsuario(usuarioId);
    }
}
