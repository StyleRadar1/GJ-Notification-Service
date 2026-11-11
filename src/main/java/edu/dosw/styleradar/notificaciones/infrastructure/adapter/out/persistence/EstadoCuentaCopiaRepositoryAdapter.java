package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.mapper.EstadoCuentaCopiaEntityMapper;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.EstadoCuentaCopiaJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataEstadoCuentaCopiaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EstadoCuentaCopiaRepositoryAdapter implements EstadoCuentaCopiaRepositoryPort {

    private final SpringDataEstadoCuentaCopiaRepository repository;
    private final EstadoCuentaCopiaEntityMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EstadoCuentaCopia guardar(EstadoCuentaCopia estadoCuenta) {
        EstadoCuentaCopiaJpaEntity entity = mapper.toEntity(estadoCuenta);
        EstadoCuentaCopiaJpaEntity guardada = repository.save(entity);
        return mapper.toDomain(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadoCuentaCopia> buscarPorUsuarioId(Long usuarioId) {
        return repository.findById(usuarioId).map(mapper::toDomain);
    }
}
