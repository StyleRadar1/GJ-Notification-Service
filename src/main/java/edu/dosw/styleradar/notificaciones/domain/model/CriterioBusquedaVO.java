package edu.dosw.styleradar.notificaciones.domain.model;

import java.math.BigDecimal;

public record CriterioBusquedaVO(
        String textoLibre,
        String categoria,
        String color,
        String talla,
        String marca,
        String estilo,
        BigDecimal precioMin,
        BigDecimal precioMax,
        String localidad
) {
}
