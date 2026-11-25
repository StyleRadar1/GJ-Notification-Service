package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** A06: con el limite encendido, la solicitud que pasa del cupo por minuto recibe 429 con ProblemDetail. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "application.rate-limit.enabled=true",
        "application.rate-limit.requests-per-minute=3"
})
@DisplayName("Limite de solicitudes por usuario")
class LimiteDeSolicitudesHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("La cuarta solicitud del minuto responde 429 y la salud sigue libre")
    void laCuartaSolicitudResponde429() throws Exception {
        String token = jwtTokenService.generateToken(9_301L, "limite@styleradar.co", "COMPRADOR");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/users/9301/notifications/unread/count").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/users/9301/notifications/unread/count").header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Demasiadas solicitudes"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }
}
