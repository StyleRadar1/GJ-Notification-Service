package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import java.util.List;

public interface PreferenciasAvisosUseCase {

    /** Una preferencia por cada tipo de aviso; los que el usuario no configuro salen como solo plataforma. */
    List<PreferenciaAviso> obtenerPreferencias(Long usuarioId);

    PreferenciaAviso guardarPreferencia(Long usuarioId, TipoNotificacion tipo, CanalAviso canal);
}
