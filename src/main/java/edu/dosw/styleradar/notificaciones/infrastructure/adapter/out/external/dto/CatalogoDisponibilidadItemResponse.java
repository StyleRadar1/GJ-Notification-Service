package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto;

import java.util.List;

public record CatalogoDisponibilidadItemResponse(
        Long itemId,
        Integer stockTotal,
        List<String> tallasConStock
) {
}
