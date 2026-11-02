package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import java.util.List;
import java.util.Optional;

public interface AlertaRepositoryPort {

    Alerta guardar(Alerta alerta);

    Optional<Alerta> buscarPorId(Long id);

    List<Alerta> listarPorUsuario(Long usuarioId);

    List<Alerta> listarActivasPorItem(Long itemId);

    List<Alerta> listarActivasPorTipo(TipoAlerta tipoAlerta);

    List<Alerta> listarActivasPorBusquedaGuardada(Long busquedaGuardadaId);

    boolean existeActivaParaItem(Long usuarioId, Long itemId, String tallaObjetivo);

    boolean existeActivaParaBusqueda(Long usuarioId, Long busquedaGuardadaId);

    int desactivarPorItem(Long itemId);

    int desactivarPorBusquedaGuardada(Long busquedaGuardadaId);

    long contarPorEstado(EstadoAlerta estado);
}
