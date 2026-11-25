package edu.dosw.styleradar.notificaciones.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
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

    /**
     * Todas las rutas exigen JWT y estan sujetas al limite por minuto, asi que cada operacion documenta 401, 403 y
     * 429 aunque el controlador no las liste, y el esquema del error trae los campos que el servicio realmente
     * devuelve (mensaje, timestamp y errors).
     */
    @Bean
    public OpenApiCustomizer erroresComunesOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Schema<?> problema = openApi.getComponents().getSchemas().get("ProblemDetail");
                if (problema != null) {
                    problema.addProperty("code", new Schema<String>().type("string")
                            ._enum(java.util.Arrays.stream(edu.dosw.styleradar.notificaciones.domain.exception.CodigoError.values())
                                    .map(Enum::name).toList())
                            .description("Stable machine-readable error code; the front chooses its message from it, not from detail"));
                    problema.addProperty("mensaje", new Schema<String>().type("string")
                            .description("Copy of detail, ready to show on screen"));
                    problema.addProperty("timestamp", new Schema<String>().type("string").format("date-time")
                            .description("Time of the error (UTC)"));
                    problema.addProperty("errors", new Schema<Object>().type("object")
                            .additionalProperties(new Schema<String>().type("string"))
                            .description("Only on validation 400s: one message per field"));
                }
            }
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(operacion -> {
                agregarSiFalta(operacion, "401", "Unauthorized: missing, invalid or expired JWT");
                agregarSiFalta(operacion, "403", "Forbidden: the {id} in the path is not the authenticated user's");
                agregarSiFalta(operacion, "429", "Too many requests: over 120 requests per minute (see the Retry-After header)");
            }));
        };
    }

    private void agregarSiFalta(io.swagger.v3.oas.models.Operation operacion, String codigo, String descripcion) {
        if (operacion.getResponses() == null || operacion.getResponses().containsKey(codigo)) {
            return;
        }
        operacion.getResponses().addApiResponse(codigo, new ApiResponse().description(descripcion)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))));
    }
}
