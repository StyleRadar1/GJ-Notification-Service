package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.NotificacionApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.NotificacionApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarNotificacionesUseCaseImpl implements ListarNotificacionesUseCase {

    private final NotificacionApplicationService notificacionApplicationService;

    public ListarNotificacionesUseCaseImpl(NotificacionRepositoryPort notificacionRepositoryPort) {
        this(new NotificacionApplicationServiceImpl(notificacionRepositoryPort));
    }

    @Override
    public List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId, Boolean soloNoLeidas) {
        return notificacionApplicationService.listarNotificacionesPorUsuario(usuarioId, soloNoLeidas);
    }

    @Override
    public List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId) {
        return notificacionApplicationService.listarPosterioresA(usuarioId, ultimoId);
    }

    @Override
    public long contarNoLeidas(Long usuarioId) {
        return notificacionApplicationService.contarNoLeidas(usuarioId);
    }
}
