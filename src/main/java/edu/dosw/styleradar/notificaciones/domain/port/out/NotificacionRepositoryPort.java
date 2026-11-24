package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepositoryPort {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(Long id);

    List<Notificacion> listarPorUsuario(Long usuarioId, Boolean soloNoLeidas);

    /**
     * Notificaciones del usuario por paginas, de la mas reciente a la mas antigua.
     *
     * @param leida {@code true} solo las leidas, {@code false} solo las no leidas, {@code null} todas
     */
    Pagina<Notificacion> listarPaginadas(Long usuarioId, Boolean leida, int page, int size);

    /** Notificaciones del usuario con id mayor al indicado, de la mas antigua a la mas reciente. */
    List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId);

    long contarNoLeidasPorUsuario(Long usuarioId);

    int marcarTodasComoLeidasPorUsuario(Long usuarioId);

    /** Borrado logico de las notificaciones leidas del usuario. Devuelve cuantas elimino. */
    int eliminarLeidasPorUsuario(Long usuarioId);

    boolean existePorAlertaItemYCiclo(Long alertaId, String itemId, Integer cicloReposicion);

    boolean existePorEventoYUsuario(String eventoId, Long usuarioId);

    /** Borra todas las notificaciones del usuario (cuenta eliminada). Devuelve cuantas borro. */
    int eliminarPorUsuario(Long usuarioId);

    /** Borra las notificaciones NO leidas de esos tipos para la prenda (ya no sirven). */
    int eliminarNoLeidasPorItemYTipos(String itemId, Collection<TipoNotificacion> tipos);

    /** Quita el enlace de las notificaciones de esos tipos para la prenda, que ya no esta disponible. */
    int quitarEnlacePorItemYTipos(String itemId, Collection<TipoNotificacion> tipos);
}
