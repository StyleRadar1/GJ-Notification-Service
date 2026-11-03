package edu.dosw.styleradar.notificaciones.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record DatosPrendaVO(
        Long itemId,
        String nombrePrenda,
        String categoria,
        String color,
        List<String> tallasDisponibles,
        String marca,
        String estilo,
        BigDecimal precio,
        Integer stockDisponible,
        Integer cicloReposicion,
        Long tiendaId,
        String localidad
) {
}
