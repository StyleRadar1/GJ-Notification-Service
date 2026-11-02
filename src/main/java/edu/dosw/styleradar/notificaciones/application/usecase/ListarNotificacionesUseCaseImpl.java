package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarNotificacionesUseCaseImpl implements ListarNotificacionesUseCase {

    private final NotificacionRepositoryPort notificacionRepositoryPort;

    @Override
    public List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId, Boolean soloNoLeidas) {
        return notificacionRepositoryPort.listarPorUsuario(usuarioId, soloNoLeidas);
    }

    @Override
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepositoryPort.contarNoLeidasPorUsuario(usuarioId);
    }
}
