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
                        .description("Documentación interactiva OpenAPI 3.0 para el Microservicio de Alertas y Notificaciones (DOSW - Equipo Guatoque Jiraguas). Permite gestionar alertas de reposición de stock, búsquedas guardadas, notificaciones multicanal y emisión en tiempo real vía Server-Sent Events (SSE).")
                        .contact(new Contact()
                                .name("StyleRadar - Equipo Notificaciones (GJ)")
                                .email("notificaciones@styleradar.co")))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8300").description("Entorno Local / Docker Compose (ms-notificaciones :8300)"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://styleradar-notificaciones-qa.azurewebsites.net").description("Entorno de Pruebas y Calidad QA (ms-notificaciones)"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://styleradar-notificaciones-prod.azurewebsites.net").description("Entorno de Producción PROD (ms-notificaciones)")
                ));
    }

    @Bean
    public OpenApiCustomizer canonicalEnglishOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                Paths filtered = new Paths();
                openApi.getPaths().forEach((path, pathItem) -> {
                    // Only display clean canonical English endpoints under /api/v1/users/{id}/ in Swagger UI
                    if (path.startsWith("/api/v1/users/{id}/")
                            && !path.contains("alertas")
                            && !path.contains("notificaciones")
                            && !path.contains("preferencias-avisos")
                            && !path.contains("leer")
                            && !path.contains("desactivar")
                            && !path.contains("conteo")
                            && !path.contains("contador")
                            && !path.contains("unread-count")
                            && !path.startsWith("/api/v1/users/{id}/preferences")) {
                        filtered.addPathItem(path, pathItem);
                    }
                });
                openApi.setPaths(filtered);
            }
        };
    }
}
