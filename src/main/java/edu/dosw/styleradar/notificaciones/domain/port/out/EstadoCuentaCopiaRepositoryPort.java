package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import java.util.Optional;

public interface EstadoCuentaCopiaRepositoryPort {

    EstadoCuentaCopia guardar(EstadoCuentaCopia estadoCuenta);

    Optional<EstadoCuentaCopia> buscarPorUsuarioId(Long usuarioId);
}
