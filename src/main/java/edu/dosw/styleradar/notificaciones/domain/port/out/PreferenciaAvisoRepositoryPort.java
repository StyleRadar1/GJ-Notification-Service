package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import java.util.List;
import java.util.Optional;

public interface PreferenciaAvisoRepositoryPort {

    Optional<PreferenciaAviso> buscar(Long usuarioId, TipoNotificacion tipo);

    List<PreferenciaAviso> listarPorUsuario(Long usuarioId);

    PreferenciaAviso guardar(PreferenciaAviso preferencia);
}
