package edu.dosw.styleradar.notificaciones.integration.adapter.out;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class CatalogoClientAdapterWireMockTest {

    private static final String RUTA_ITEM = "/api/v1/catalogo/items/1045/disponibilidad";

    @RegisterExtension
    static WireMockExtension catalogo = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configurarCatalogo(DynamicPropertyRegistry registry) {
        registry.add("application.clients.catalog-url", catalogo::baseUrl);
    }

    @Autowired
    private CatalogoClientPort catalogoClientPort;

    @Test
    @DisplayName("NOT-10 - Debe considerar agotado un item sin stock en Catalogo")
    void debeConsiderarAgotadoUnItemSinStock() {
        catalogo.stubFor(get(urlEqualTo(RUTA_ITEM)).willReturn(json(
                "{\"itemId\":1045,\"stockTotal\":0,\"tallasConStock\":[]}")));

        assertThat(catalogoClientPort.verificarItemAgotado(1045L, null)).isTrue();
    }

    @Test
    @DisplayName("NOT-10 - Debe considerar disponible un item con stock en Catalogo")
    void debeConsiderarDisponibleUnItemConStock() {
        catalogo.stubFor(get(urlEqualTo(RUTA_ITEM)).willReturn(json(
                "{\"itemId\":1045,\"stockTotal\":8,\"tallasConStock\":[\"M\",\"L\"]}")));

        assertThat(catalogoClientPort.verificarItemAgotado(1045L, null)).isFalse();
        assertThat(catalogoClientPort.verificarItemAgotado(1045L, "m")).isFalse();
        assertThat(catalogoClientPort.verificarItemAgotado(1045L, "XL")).isTrue();
    }

    @Test
    @DisplayName("NOT-10 - Debe rechazar con regla de negocio un item que Catalogo no conoce")
    void debeRechazarItemInexistente() {
        catalogo.stubFor(get(urlEqualTo(RUTA_ITEM)).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> catalogoClientPort.verificarItemAgotado(1045L, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("1045");
    }

    @Test
    @DisplayName("NOT-10 - Debe indicar servicio no disponible cuando Catalogo responde con error")
    void debeIndicarServicioNoDisponibleCuandoCatalogoFalla() {
        catalogo.stubFor(get(urlEqualTo(RUTA_ITEM)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> catalogoClientPort.verificarItemAgotado(1045L, null))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    @Test
    @DisplayName("NOT-10 - Debe indicar servicio no disponible cuando la conexion con Catalogo se corta")
    void debeIndicarServicioNoDisponibleCuandoSeCortaLaConexion() {
        catalogo.stubFor(get(urlEqualTo(RUTA_ITEM))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> catalogoClientPort.verificarItemAgotado(1045L, null))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(String cuerpo) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(cuerpo);
    }
}
