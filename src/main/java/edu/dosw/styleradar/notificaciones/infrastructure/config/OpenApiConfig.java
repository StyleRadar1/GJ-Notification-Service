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
                        .title("StyleRadar - Microservicio de Notificaciones API")
                        .version("1.0.0")
                        .description("API REST y SSE para gestion de alertas de disponibilidad, busquedas guardadas y notificaciones en tiempo real")
                        .contact(new Contact()
                                .name("Equipo StyleRadar - DOSW")
                                .email("soporte@styleradar.co")));
    }
}
