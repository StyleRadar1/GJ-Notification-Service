package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepositoryPort {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(Long id);

    List<Notificacion> listarPorUsuario(Long usuarioId, Boolean soloNoLeidas);

    /** Notificaciones del usuario con id mayor al indicado, de la mas antigua a la mas reciente. */
    List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId);

    long contarNoLeidasPorUsuario(Long usuarioId);

    int marcarTodasComoLeidasPorUsuario(Long usuarioId);

    boolean existePorAlertaItemYCiclo(Long alertaId, Long itemId, Integer cicloReposicion);
}
