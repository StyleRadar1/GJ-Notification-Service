package edu.dosw.styleradar.notificaciones.application.mapper.in;

import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.AlertaResponse;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlertaDtoMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "usuarioId", target = "usuarioId")
    @Mapping(source = "request.tipoAlerta", target = "tipoAlerta")
    @Mapping(source = "request.itemId", target = "itemId")
    @Mapping(source = "request.tallaObjetivo", target = "tallaObjetivo")
    @Mapping(source = "request.busquedaGuardadaId", target = "busquedaGuardadaId")
    @Mapping(source = "request.canal", target = "canal")
    Alerta toDomain(CrearAlertaRequest request, Long usuarioId);

    AlertaResponse toResponse(Alerta domain);
}
