package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.mapper;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.EstadoCuentaCopiaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstadoCuentaCopiaEntityMapper {

    EstadoCuentaCopiaJpaEntity toEntity(EstadoCuentaCopia domain);

    EstadoCuentaCopia toDomain(EstadoCuentaCopiaJpaEntity entity);
}
