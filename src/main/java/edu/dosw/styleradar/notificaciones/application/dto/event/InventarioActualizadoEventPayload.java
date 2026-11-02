package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventarioActualizadoEventPayload(
        String eventoId,
        Long itemId,
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
        Long tiendaId,
        String localidad,
        LocalDateTime fechaEvento
) {
}
