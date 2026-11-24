package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.CodigoError;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.EliminarNotificacionUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * El borrado es logico: la fila se conserva con su fecha de eliminacion para que la restriccion de entregas
 * repetidas siga vigente y un evento reentregado no vuelva a crear la notificacion.
 */
@Service
@RequiredArgsConstructor
public class EliminarNotificacionUseCaseImpl implements EliminarNotificacionUseCase {

    private final NotificacionRepositoryPort notificacionRepositoryPort;

    @Override
    public void eliminar(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = notificacionRepositoryPort.buscarPorId(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro la notificacion", CodigoError.NOTIFICATION_NOT_FOUND));
        if (!notificacion.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tiene permisos para eliminar una notificacion de otro usuario");
        }
        notificacion.marcarComoEliminada();
        notificacionRepositoryPort.guardar(notificacion);
    }

    @Override
    public int eliminarLeidas(Long usuarioId) {
        return notificacionRepositoryPort.eliminarLeidasPorUsuario(usuarioId);
    }

    @Override
    public int eliminarSeleccionadas(Long usuarioId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ReglaDeNegocioException("Indique al menos una notificacion para eliminar");
        }
        Set<Long> distintos = new LinkedHashSet<>(ids);
        if (distintos.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ReglaDeNegocioException("Los ids de notificacion deben ser numeros positivos");
        }
        if (distintos.size() > MAXIMO_POR_SOLICITUD) {
            throw new ReglaDeNegocioException(
                    "Se pueden eliminar como maximo " + MAXIMO_POR_SOLICITUD + " notificaciones por solicitud");
        }
        return notificacionRepositoryPort.eliminarSeleccionadasPorUsuario(usuarioId, distintos);
    }
}
