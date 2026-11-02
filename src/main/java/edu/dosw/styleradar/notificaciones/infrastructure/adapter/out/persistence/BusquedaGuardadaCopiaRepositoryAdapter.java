package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.application.mapper.out.BusquedaGuardadaCopiaEntityMapper;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.BusquedaGuardadaCopiaJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataBusquedaGuardadaCopiaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BusquedaGuardadaCopiaRepositoryAdapter implements BusquedaGuardadaCopiaRepositoryPort {

    private final SpringDataBusquedaGuardadaCopiaRepository repository;
    private final BusquedaGuardadaCopiaEntityMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusquedaGuardadaCopia guardar(BusquedaGuardadaCopia copia) {
        BusquedaGuardadaCopiaJpaEntity entity = mapper.toEntity(copia);
        BusquedaGuardadaCopiaJpaEntity guardada = repository.save(entity);
        return mapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusquedaGuardadaCopia> buscarPorId(Long busquedaId) {
        return repository.findById(busquedaId).map(mapper::toDomain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long busquedaId) {
        repository.deleteById(busquedaId);
    }
}
