package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PrendaPublicadaEventPayload(
        String eventoId,
        Long itemId,
        String nombrePrenda,
        String categoria,
        String color,
        List<String> tallasDisponibles,
        String marca,
        String estilo,
        BigDecimal precio,
        Integer stockTotal,
        Long tiendaId,
        String localidad,
        LocalDateTime fechaPublicacion
) {
}
