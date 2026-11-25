package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.Test;

class CatalogoFeignConfigTest {

    private final CatalogoFeignConfig config = new CatalogoFeignConfig();

    @Test
    void normalizesBaseUrlWithTrailingApiV1() {
        assertThat(CatalogoFeignConfig.normalizeBaseUrl("http://localhost:8220/api/v1"))
                .isEqualTo("http://localhost:8220");
        assertThat(CatalogoFeignConfig.normalizeBaseUrl("http://localhost:8220/api/v1/"))
                .isEqualTo("http://localhost:8220");
        assertThat(CatalogoFeignConfig.normalizeBaseUrl("https://catalogo.qa.azurewebsites.net/"))
                .isEqualTo("https://catalogo.qa.azurewebsites.net");
        assertThat(CatalogoFeignConfig.normalizeBaseUrl(null)).isNull();
    }

    @Test
    void interceptorPreventsDoubleApiV1WhenBaseUrlContainsPrefix() {
        RequestInterceptor interceptor = config.catalogUrlNormalizationInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.feignTarget(new Target.HardCodedTarget<>(
                CatalogoFeignClient.class, "ms-item-catalogo", "https://catalogo.qa/api/v1/"));
        template.uri("/api/v1/garments/1045/availability");

        interceptor.apply(template);

        assertThat(template.url()).isEqualTo("https://catalogo.qa/api/v1/garments/1045/availability");
    }

    @Test
    void interceptorCollapsesExistingDoublePrefixInPath() {
        RequestInterceptor interceptor = config.catalogUrlNormalizationInterceptor();
        RequestTemplate template = new RequestTemplate();
        template.uri("/api/v1/api/v1/garments/1045/availability");

        interceptor.apply(template);

        assertThat(template.url()).isEqualTo("/api/v1/garments/1045/availability");
    }
}
