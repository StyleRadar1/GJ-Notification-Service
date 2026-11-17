package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Con SSE_QUERY_TOKEN_ENABLED=false el stream solo acepta la cabecera Authorization. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "application.security.sse-query-token.enabled=false")
@DisplayName("Stream SSE con el token en la consulta desactivado")
class SseTokenPorConsultaDesactivadoHttpTest {

    private static final String STREAM = "/api/v1/usuarios/me/notificaciones/stream";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("El token en la consulta responde 401 y la cabecera sigue funcionando")
    void soloLaCabeceraFunciona() throws Exception {
        String token = jwtTokenService.generateToken(9_102L, "sse2@styleradar.co", "COMPRADOR");

        mockMvc.perform(get(STREAM).param("access_token", token)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
