package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtAuthenticationFilter;
import edu.dosw.styleradar.notificaciones.infrastructure.security.ProblemDetailSecurityHandlers;
import edu.dosw.styleradar.notificaciones.infrastructure.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProblemDetailSecurityHandlers securityHandlers;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${application.cors.allowed-origins:}") List<String> origenesPermitidos
    ) throws Exception {
        List<String> origenesLimpios = origenesPermitidos.stream()
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();

        http.csrf(AbstractHttpConfigurer::disable);
        if (origenesLimpios.isEmpty()) {
            // Detras del API Gateway no emitimos cabeceras CORS para evitar Access-Control-Allow-Origin duplicada
            http.cors(AbstractHttpConfigurer::disable);
        } else {
            http.cors(cors -> cors.configurationSource(construirFuenteCors(origenesLimpios)));
        }

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(securityHandlers.accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        // metricas: solo administradores (A02/A09); health sigue publico para el health check de Azure
                        .requestMatchers("/actuator/metrics/**", "/actuator/prometheus")
                        .hasAnyRole("ADMIN", "ADMIN_STYLERADAR", "STYLERADAR_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS opcional para desarrollo local directo sin API Gateway. Cuando CORS_ALLOWED_ORIGINS esta vacio
     * (QA y PROD), el API Gateway es el unico responsable de emitir las cabeceras CORS.
     */
    private CorsConfigurationSource construirFuenteCors(List<String> origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Last-Event-ID"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
