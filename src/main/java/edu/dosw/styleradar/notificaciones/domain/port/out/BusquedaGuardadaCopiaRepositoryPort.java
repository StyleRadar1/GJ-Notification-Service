package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import java.util.Optional;

public interface BusquedaGuardadaCopiaRepositoryPort {

    BusquedaGuardadaCopia guardar(BusquedaGuardadaCopia copia);

    Optional<BusquedaGuardadaCopia> buscarPorId(Long busquedaId);

    void eliminar(Long busquedaId);
}
