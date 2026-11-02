package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MarcarNotificacionLeidaUseCaseImpl implements MarcarNotificacionLeidaUseCase {

    private final NotificacionRepositoryPort notificacionRepositoryPort;

    @Override
    public Notificacion marcarComoLeida(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = notificacionRepositoryPort.buscarPorId(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la notificacion con ID: " + notificacionId));

        if (!notificacion.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tiene permisos para modificar una notificacion de otro usuario");
        }

        notificacion.marcarComoLeida();
        return notificacionRepositoryPort.guardar(notificacion);
    }

    @Override
    public int marcarTodasComoLeidas(Long usuarioId) {
        return notificacionRepositoryPort.marcarTodasComoLeidasPorUsuario(usuarioId);
    }
}
