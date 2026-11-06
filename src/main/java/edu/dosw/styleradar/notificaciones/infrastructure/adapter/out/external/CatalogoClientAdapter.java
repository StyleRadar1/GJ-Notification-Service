package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external;

import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import feign.FeignException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoClientAdapter implements CatalogoClientPort {

    private static final String MENSAJE_NO_DISPONIBLE =
            "El catalogo no esta disponible en este momento, intente nuevamente mas tarde";

    private final CatalogoFeignClient catalogoFeignClient;

    @Override
    public boolean verificarItemAgotado(Long itemId, String tallaObjetivo) {
        CatalogoDisponibilidadItemResponse disp = consultarDisponibilidad(itemId);
        if (tallaObjetivo != null && !tallaObjetivo.isBlank() && disp.tallasConStock() != null) {
            String norm = tallaObjetivo.trim().toUpperCase(Locale.ROOT);
            boolean tieneTalla = disp.tallasConStock().stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(t -> t.trim().toUpperCase(Locale.ROOT))
                    .anyMatch(norm::equals);
            return !tieneTalla;
        }
        return disp.stockTotal() == null || disp.stockTotal() <= 0;
    }

    private CatalogoDisponibilidadItemResponse consultarDisponibilidad(Long itemId) {
        CatalogoDisponibilidadItemResponse disp;
        try {
            disp = catalogoFeignClient.consultarDisponibilidad(itemId);
        } catch (FeignException.NotFound ex) {
            throw new ReglaDeNegocioException("El item " + itemId + " no existe en el catalogo");
        } catch (Exception ex) {
            log.warn("No fue posible consultar ms-item-catalogo para el item {}: {}", itemId, ex.getMessage());
            throw new ServicioExternoNoDisponibleException(MENSAJE_NO_DISPONIBLE, ex);
        }
        if (disp == null) {
            log.warn("ms-item-catalogo respondio sin datos para el item {}", itemId);
            throw new ServicioExternoNoDisponibleException(MENSAJE_NO_DISPONIBLE);
        }
        return disp;
    }
}
