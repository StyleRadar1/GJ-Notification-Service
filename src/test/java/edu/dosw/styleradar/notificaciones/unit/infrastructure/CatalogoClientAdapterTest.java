package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.domain.exception.CodigoError;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoClientAdapter;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoFeignClient;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogoClientAdapterTest {

    @Mock
    private CatalogoFeignClient feignClient;

    @InjectMocks
    private CatalogoClientAdapter adapter;

    @Test
    void maps404And400ToItemNotFoundBusinessRule() {
        when(feignClient.consultarDisponibilidad("404")).thenThrow(feignError(404));
        assertThatThrownBy(() -> adapter.verificarItemAgotado("404", null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaDeNegocioException) ex).getCodigo())
                        .isEqualTo(CodigoError.ITEM_NOT_FOUND));

        when(feignClient.consultarDisponibilidad("400")).thenThrow(feignError(400));
        assertThatThrownBy(() -> adapter.verificarItemAgotado("400", null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .satisfies(ex -> assertThat(((ReglaDeNegocioException) ex).getCodigo())
                        .isEqualTo(CodigoError.ITEM_NOT_FOUND));
    }

    @Test
    void mapsUnexpectedHttpErrorsAndNullResponseToCatalogUnavailable() {
        when(feignClient.consultarDisponibilidad("500")).thenThrow(feignError(500));
        assertThatThrownBy(() -> adapter.verificarItemAgotado("500", null))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .satisfies(ex -> assertThat(((ServicioExternoNoDisponibleException) ex).getCodigo())
                        .isEqualTo(CodigoError.CATALOG_UNAVAILABLE));

        when(feignClient.consultarDisponibilidad("null-body")).thenReturn(null);
        assertThatThrownBy(() -> adapter.verificarItemAgotado("null-body", null))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .satisfies(ex -> assertThat(((ServicioExternoNoDisponibleException) ex).getCodigo())
                        .isEqualTo(CodigoError.CATALOG_UNAVAILABLE));
    }

    @Test
    void evaluatesSizeAvailabilityCaseInsensitively() {
        when(feignClient.consultarDisponibilidad("10"))
                .thenReturn(new CatalogoDisponibilidadItemResponse("10", 5, List.of("m", "l")));

        assertThat(adapter.verificarItemAgotado("10", "M")).isFalse();
        assertThat(adapter.verificarItemAgotado("10", "XL")).isTrue();
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://catalogo/api/v1/garments/1/availability", Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("err")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("CatalogoFeignClient#consultarDisponibilidad", response);
    }
}
