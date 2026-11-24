package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
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
     * Acepta el formato plano y el sobre {@code data} de ms-Inventory (prendaId, sedeId, stockDisponible,
     * timestamp).
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static InventarioActualizadoEventPayload desdeJson(JsonNode nodo) {
        EventoJsonLector lector = new EventoJsonLector(nodo);
        return new InventarioActualizadoEventPayload(
                lector.texto("eventoId", "eventId"),
                lector.texto("itemId", "prendaId"),
                lector.texto("nombrePrenda"),
                lector.texto("categoria"),
                lector.texto("color"),
                lector.texto("talla"),
                lector.texto("marca"),
                lector.texto("estilo"),
                lector.decimal("precio"),
                lector.entero("stockAnterior"),
                lector.entero("stockActual", "stockDisponible"),
                lector.entero("cicloReposicion"),
                lector.texto("tiendaId", "sedeId"),
                lector.texto("localidad"),
                lector.fecha("fechaEvento", "timestamp", "ocurridoEn"));
    }

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
