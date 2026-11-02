package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "ms-item-catalogo",
        url = "${application.clients.catalog-url:http://localhost:8085}"
)
public interface CatalogoFeignClient {

    @GetMapping("/api/v1/catalogo/items/{itemId}/disponibilidad")
    CatalogoDisponibilidadItemResponse consultarDisponibilidad(@PathVariable("itemId") Long itemId);
}
