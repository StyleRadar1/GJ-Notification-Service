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
import org.springframework.test.web.servlet.MockMvc;

/** Los errores llevan el mismo identificador de correlacion en la cabecera y en el cuerpo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Identificador de correlacion en las respuestas de error")
class CorrelationIdHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("401 del filtro de seguridad incluye el correlationId recibido")
    void errorDeSeguridadIncluyeElIdRecibido() throws Exception {
        mockMvc.perform(get("/api/v1/users/101/notifications").header("X-Correlation-Id", "traza-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-Id", "traza-401"))
                .andExpect(jsonPath("$.correlationId").value("traza-401"));
    }

    @Test
    @DisplayName("403 de la API incluye el correlationId recibido")
    void errorDeLaApiIncluyeElIdRecibido() throws Exception {
        String token = jwtTokenService.generateToken(101L, "usuario101@styleradar.co", "COMPRADOR");

        mockMvc.perform(get("/api/v1/users/102/notifications")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-Id", "traza-403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.correlationId").value("traza-403"));
    }
}
