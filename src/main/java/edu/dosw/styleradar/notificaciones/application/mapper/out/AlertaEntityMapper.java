package edu.dosw.styleradar.notificaciones.application.mapper.out;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.AlertaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AlertaEntityMapper {

    AlertaJpaEntity toEntity(Alerta domain);

    Alerta toDomain(AlertaJpaEntity entity);
}
