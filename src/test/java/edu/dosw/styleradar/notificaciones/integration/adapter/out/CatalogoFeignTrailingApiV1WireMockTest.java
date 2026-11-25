package edu.dosw.styleradar.notificaciones.integration.adapter.out;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
class CatalogoFeignTrailingApiV1WireMockTest {

    @RegisterExtension
    static WireMockExtension catalogo = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configurarCatalogoConApiV1(DynamicPropertyRegistry registry) {
        registry.add("application.clients.catalog-url", () -> catalogo.baseUrl() + "/api/v1");
    }

    @Autowired
    private CatalogoClientPort catalogoClientPort;

    @Test
    @DisplayName("BUG-007 - No duplica /api/v1 cuando CATALOG_SERVICE_URL ya incluye /api/v1")
    void noDuplicaPrefijoApiV1CuandoBaseUrlLoIncluye() {
        catalogo.stubFor(get(urlEqualTo("/api/v1/garments/1045/availability"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"itemId\":\"1045\",\"stockTotal\":3,\"tallasConStock\":[\"M\"]}")));

        assertThat(catalogoClientPort.verificarItemAgotado("1045", "M")).isFalse();
        catalogo.verify(1, getRequestedFor(urlEqualTo("/api/v1/garments/1045/availability")));
    }
}
