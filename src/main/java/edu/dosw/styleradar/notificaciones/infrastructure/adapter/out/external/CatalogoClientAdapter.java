package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external;

import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoClientAdapter implements CatalogoClientPort {

    private final CatalogoFeignClient catalogoFeignClient;

    @Override
    public boolean verificarItemAgotado(Long itemId, String tallaObjetivo) {
        try {
            CatalogoDisponibilidadItemResponse disp = catalogoFeignClient.consultarDisponibilidad(itemId);
            if (disp == null) {
                return true;
            }
            if (tallaObjetivo != null && !tallaObjetivo.isBlank() && disp.tallasConStock() != null) {
                String norm = tallaObjetivo.trim().toUpperCase(Locale.ROOT);
                boolean tieneTalla = disp.tallasConStock().stream()
                        .filter(t -> t != null && !t.isBlank())
                        .map(t -> t.trim().toUpperCase(Locale.ROOT))
                        .anyMatch(norm::equals);
                return !tieneTalla;
            }
            return disp.stockTotal() == null || disp.stockTotal() <= 0;
        } catch (Exception ex) {
            log.warn("No fue posible contactar a ms-item-catalogo para el item {}: {}. Se asume agotado en modo aislado.",
                    itemId, ex.getMessage());
            return true;
        }
    }
}
