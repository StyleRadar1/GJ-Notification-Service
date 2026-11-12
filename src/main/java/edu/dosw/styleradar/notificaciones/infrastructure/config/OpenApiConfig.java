package edu.dosw.styleradar.notificaciones.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                        .title("StyleRadar - Microservicio de Notificaciones API (ms-notificaciones)")
                        .version("1.0.0")
                        .description("Documentacion interactiva oficial OpenAPI 3.0 para el Microservicio de Notificaciones y Alertas (DOSW - Equipo Guatoque Jiraguas). Gestiona de forma autonoma las alertas de prendas agotadas, busquedas guardadas, notificaciones multicanal y conexion reactiva SSE.")
                        .contact(new Contact()
                                .name("Equipo Guatoque Jiraguas (GJ) - StyleRadar")
                                .email("notificaciones@styleradar.co")))
                .servers(java.util.List.of(
                        new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8300").description("Entorno Local / Docker Compose (ms-notificaciones :8300)"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://styleradar-notificaciones-qa.azurewebsites.net").description("Entorno de Calidad QA (ms-notificaciones)")
                ));
    }
}
