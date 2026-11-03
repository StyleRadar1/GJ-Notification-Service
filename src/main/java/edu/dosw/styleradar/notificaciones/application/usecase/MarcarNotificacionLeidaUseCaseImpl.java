package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.NotificacionApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.NotificacionApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MarcarNotificacionLeidaUseCaseImpl implements MarcarNotificacionLeidaUseCase {

    private final NotificacionApplicationService notificacionApplicationService;

    public MarcarNotificacionLeidaUseCaseImpl(NotificacionRepositoryPort notificacionRepositoryPort) {
        this(new NotificacionApplicationServiceImpl(notificacionRepositoryPort));
    }

    @Override
    public Notificacion marcarComoLeida(Long notificacionId, Long usuarioId) {
        return notificacionApplicationService.marcarComoLeida(notificacionId, usuarioId);
    }

    @Override
    public int marcarTodasComoLeidas(Long usuarioId) {
        return notificacionApplicationService.marcarTodasComoLeidas(usuarioId);
    }
}
