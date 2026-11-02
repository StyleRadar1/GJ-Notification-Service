package edu.dosw.styleradar.notificaciones.application.mapper.out;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.NotificacionJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionEntityMapper {

    NotificacionJpaEntity toEntity(Notificacion domain);

    Notificacion toDomain(NotificacionJpaEntity entity);
}
