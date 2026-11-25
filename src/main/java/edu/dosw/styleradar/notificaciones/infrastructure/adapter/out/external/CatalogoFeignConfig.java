package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external;

import feign.RequestInterceptor;
import feign.Target;
import org.springframework.context.annotation.Bean;

/**
 * Configuracion Feign especifica para {@link CatalogoFeignClient}. Normaliza la URL base para
 * evitar rutas duplicadas {@code /api/v1/api/v1/...} cuando {@code CATALOG_SERVICE_URL} ya
 * incluye el prefijo {@code /api/v1}.
 */
public class CatalogoFeignConfig {

    private static final String API_V1 = "/api/v1";

    @Bean
    public RequestInterceptor catalogUrlNormalizationInterceptor() {
        return template -> {
            String url = template.url();
            if (url != null) {
                String collapsed = collapseDoubleApiPrefix(url);
                if (!collapsed.equals(url)) {
                    template.uri(collapsed);
                }
            }
            Target<?> target = template.feignTarget();
            String baseUrl = target != null ? normalizeBaseUrl(target.url()) : null;
            if (baseUrl != null && !baseUrl.isBlank() && !template.url().startsWith("http")) {
                template.target(baseUrl);
            }
        };
    }

    public static String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String normalized = rawUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(API_V1)) {
            normalized = normalized.substring(0, normalized.length() - API_V1.length());
        }
        return normalized;
    }

    public static String collapseDoubleApiPrefix(String path) {
        if (path == null) {
            return null;
        }
        String collapsed = path;
        while (collapsed.contains("/api/v1/api/v1/")) {
            collapsed = collapsed.replace("/api/v1/api/v1/", "/api/v1/");
        }
        if (collapsed.endsWith("/api/v1/api/v1")) {
            collapsed = collapsed.substring(0, collapsed.length() - API_V1.length());
        }
        return collapsed;
    }
}
