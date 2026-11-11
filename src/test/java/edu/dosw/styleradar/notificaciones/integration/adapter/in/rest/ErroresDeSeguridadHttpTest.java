package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Los 401 del filtro de seguridad traen el mismo cuerpo ProblemDetail que el resto de errores. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Errores 401 con cuerpo ProblemDetail")
class ErroresDeSeguridadHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sin token: 401 con titulo, detalle, mensaje, instancia y fecha")
    void sinTokenTraeProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me/alertas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.mensaje").value("Token JWT ausente, invalido o expirado"))
                .andExpect(jsonPath("$.instance").value("/api/v1/usuarios/me/alertas"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Con un token invalido: el mismo 401 con cuerpo")
    void tokenInvalidoTraeProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }
}
