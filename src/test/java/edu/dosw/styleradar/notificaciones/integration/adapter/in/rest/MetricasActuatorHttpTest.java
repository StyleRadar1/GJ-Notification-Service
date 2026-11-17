package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** A02 y A09: las metricas solo las lee un administrador y traen las metricas propias del servicio. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
@DisplayName("Metricas de Prometheus protegidas")
class MetricasActuatorHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Sin token, /actuator/prometheus responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con un usuario normal, /actuator/prometheus y /actuator/metrics responden 403")
    void usuarioNormalResponde403() throws Exception {
        String token = jwtTokenService.generateToken(9_401L, "comprador@styleradar.co", "COMPRADOR");
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrador lee las metricas propias: campanas conectadas, correos pendientes y 429")
    void administradorLeeLasMetricas() throws Exception {
        String token = jwtTokenService.generateToken(9_402L, "admin@styleradar.co", "ADMIN");
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("notificaciones_sse_conexiones_activas")))
                .andExpect(content().string(Matchers.containsString("notificaciones_correos_pendientes")))
                .andExpect(content().string(Matchers.containsString("notificaciones_ratelimit_rechazadas_total")));
        mockMvc.perform(get("/actuator/metrics/notificaciones.sse.conexiones.activas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
