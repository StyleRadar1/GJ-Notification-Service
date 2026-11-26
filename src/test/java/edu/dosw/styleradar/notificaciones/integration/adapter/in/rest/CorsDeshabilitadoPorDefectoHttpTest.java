package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Con CORS_ALLOWED_ORIGINS vacio por defecto (QA y PROD detras del API Gateway), el servicio no emite
 * Access-Control-Allow-Origin para no duplicar la cabecera que ya agrega el Gateway (incluyendo SSE).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CORS delegado al API Gateway por defecto")
class CorsDeshabilitadoPorDefectoHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sin CORS_ALLOWED_ORIGINS no emite Access-Control-Allow-Origin ni en REST ni en stream SSE")
    void noEmiteCabeceraCorsDetrasDelGateway() throws Exception {
        String token = jwtTokenService.generateToken(6_602L, "gateway@styleradar.co", "COMPRADOR");

        mockMvc.perform(get("/api/v1/users/6602/notifications")
                        .header("Origin", "https://styleradar-qa.vercel.app")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        String ticketJson = mockMvc.perform(post("/api/v1/users/6602/notifications/stream-ticket")
                        .header("Origin", "https://styleradar-qa.vercel.app")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andReturn().getResponse().getContentAsString();
        String ticket = objectMapper.readTree(ticketJson).get("ticket").asText();

        mockMvc.perform(get("/api/v1/users/6602/notifications/stream")
                        .param("ticket", ticket)
                        .header("Origin", "https://styleradar-qa.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
