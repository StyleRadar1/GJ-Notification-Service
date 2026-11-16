package edu.dosw.styleradar.notificaciones.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StyleRadar - Notification Service API (ms-notificaciones)")
                        .version("1.0.0")
                        .description("Official OpenAPI 3.0 interactive documentation for the Notification & Alert Microservice (DOSW - Equipo Guatoque Jiraguas). Manages stock alerts, saved searches, multichannel notifications and real-time SSE stream.")
                        .contact(new Contact()
                                .name("StyleRadar - Squad Notificaciones (GJ)")
                                .email("notificaciones@styleradar.co")))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8300").description("Local Environment / Docker Compose (ms-notificaciones :8300)"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://styleradar-notificaciones-qa.azurewebsites.net").description("QA Quality & Staging Environment (ms-notificaciones)"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://styleradar-notificaciones-prod.azurewebsites.net").description("PROD Production Environment (ms-notificaciones)")
                ));
    }

    @Bean
    public OpenApiCustomizer canonicalEnglishOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                Paths filtered = new Paths();
                openApi.getPaths().forEach((path, pathItem) -> {
                    // Only display clean canonical English endpoints under /users/{id}/ in Swagger UI
                    if (path.startsWith("/users/{id}/")
                            && !path.contains("alertas")
                            && !path.contains("notificaciones")
                            && !path.contains("preferencias-avisos")
                            && !path.contains("leer")
                            && !path.contains("desactivar")
                            && !path.contains("conteo")
                            && !path.contains("contador")
                            && !path.contains("unread-count")
                            && !path.startsWith("/users/{id}/preferences")) {
                        filtered.addPathItem(path, pathItem);
                    }
                });
                openApi.setPaths(filtered);
            }
        };
    }
}
