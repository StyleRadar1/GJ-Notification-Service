package edu.dosw.styleradar.notificaciones.application.mapper.in;

import edu.dosw.styleradar.notificaciones.application.dto.response.NotificacionResponse;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacionDtoMapper {

    NotificacionResponse toResponse(Notificacion domain);
}
