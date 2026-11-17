package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventarioActualizadoEventPayload(
        String eventoId,
        String itemId,
        String nombrePrenda,
        String categoria,
        String color,
        String talla,
        String marca,
        String estilo,
        BigDecimal precio,
        Integer stockAnterior,
        Integer stockActual,
        Integer cicloReposicion,
        String tiendaId,
        String localidad,
        LocalDateTime fechaEvento
) {

    /**
     * Hay reposicion cuando la prenda vuelve a tener unidades disponibles.
     * Si inventario no envia stockAnterior (backlog RES-133), cualquier stockActual positivo
     * se evalua como disponibilidad de stock.
     */
    public boolean esReposicion() {
        if (stockActual == null || stockActual <= 0) {
            return false;
        }
        if (stockAnterior == null) {
            return true;
        }
        return stockAnterior == 0;
    }
}
