package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.application.mapper.out.AlertaEntityMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.AlertaJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataAlertaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AlertaRepositoryAdapter implements AlertaRepositoryPort {

    private final SpringDataAlertaRepository repository;
    private final AlertaEntityMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Alerta guardar(Alerta alerta) {
        AlertaJpaEntity entity = mapper.toEntity(alerta);
        AlertaJpaEntity guardada = repository.save(entity);
        return mapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alerta> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alerta> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alerta> listarActivasPorItem(Long itemId) {
        return repository.findByItemIdAndEstado(itemId, EstadoAlerta.ACTIVA)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alerta> listarActivasPorTipo(TipoAlerta tipoAlerta) {
        return repository.findByTipoAlertaAndEstado(tipoAlerta, EstadoAlerta.ACTIVA)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alerta> listarActivasPorBusquedaGuardada(Long busquedaGuardadaId) {
        return repository.findByBusquedaGuardadaIdAndEstado(busquedaGuardadaId, EstadoAlerta.ACTIVA)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeActivaParaItem(Long usuarioId, Long itemId, String tallaObjetivo) {
        if (tallaObjetivo == null || tallaObjetivo.isBlank()) {
            return repository.existsByUsuarioIdAndItemIdAndEstado(usuarioId, itemId, EstadoAlerta.ACTIVA);
        }
        return repository.existsByUsuarioIdAndItemIdAndTallaObjetivoAndEstado(
                usuarioId, itemId, tallaObjetivo, EstadoAlerta.ACTIVA);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeActivaParaBusqueda(Long usuarioId, Long busquedaGuardadaId) {
        return repository.existsByUsuarioIdAndBusquedaGuardadaIdAndEstado(
                usuarioId, busquedaGuardadaId, EstadoAlerta.ACTIVA);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int desactivarPorItem(Long itemId) {
        return repository.actualizarEstadoPorItem(
                itemId, EstadoAlerta.ACTIVA, EstadoAlerta.DESACTIVADA, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int desactivarPorBusquedaGuardada(Long busquedaGuardadaId) {
        return repository.actualizarEstadoPorBusquedaGuardada(
                busquedaGuardadaId, EstadoAlerta.ACTIVA, EstadoAlerta.DESACTIVADA, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPorEstado(EstadoAlerta estado) {
        return repository.countByEstado(estado);
    }
}
