package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.mapper;

import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.BusquedaGuardadaCopiaJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BusquedaGuardadaCopiaEntityMapper {

    @Mapping(source = "criterios.textoLibre", target = "textoLibre")
    @Mapping(source = "criterios.categoria", target = "categoria")
    @Mapping(source = "criterios.color", target = "color")
    @Mapping(source = "criterios.talla", target = "talla")
    @Mapping(source = "criterios.marca", target = "marca")
    @Mapping(source = "criterios.estilo", target = "estilo")
    @Mapping(source = "criterios.precioMin", target = "precioMin")
    @Mapping(source = "criterios.precioMax", target = "precioMax")
    @Mapping(source = "criterios.localidad", target = "localidad")
    BusquedaGuardadaCopiaJpaEntity toEntity(BusquedaGuardadaCopia domain);

    default BusquedaGuardadaCopia toDomain(BusquedaGuardadaCopiaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        CriterioBusquedaVO criterios = new CriterioBusquedaVO(
                entity.getTextoLibre(),
                entity.getCategoria(),
                entity.getColor(),
                entity.getTalla(),
                entity.getMarca(),
                entity.getEstilo(),
                entity.getPrecioMin(),
                entity.getPrecioMax(),
                entity.getLocalidad()
        );
        return new BusquedaGuardadaCopia(
                entity.getBusquedaId(),
                entity.getUsuarioId(),
                entity.getNombre(),
                criterios,
                entity.isActiva(),
                entity.getFechaSincronizacion()
        );
    }
}
