package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BusquedaGuardadaSincronizadaEventPayload(
        String eventoId,
        Long busquedaId,
        Long usuarioId,
        String nombre,
        String textoLibre,
        String categoria,
        String color,
        String talla,
        String marca,
        String estilo,
        BigDecimal precioMin,
        BigDecimal precioMax,
        String localidad,
        String accion,
        LocalDateTime fechaEvento
) {
}
