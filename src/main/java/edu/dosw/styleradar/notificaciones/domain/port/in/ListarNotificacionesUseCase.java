package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import java.util.List;

public interface ListarNotificacionesUseCase {
    List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId, Boolean soloNoLeidas);
    long contarNoLeidas(Long usuarioId);

    /** Una pagina de notificaciones; {@code leida} nulo trae leidas y no leidas. */
    Pagina<Notificacion> listarNotificacionesPaginadas(Long usuarioId, Boolean leida, int page, int size);

    /** Notificaciones posteriores al id indicado (las mas recientes, hasta un tope), de antigua a reciente. */
    List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId);
}
