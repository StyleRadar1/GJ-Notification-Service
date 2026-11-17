package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El EventSource del navegador no puede enviar la cabecera Authorization: el stream SSE tambien acepta el
 * token en {@code ?access_token=}, pero ningun otro endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Stream SSE con el token en la consulta")
class SseTokenPorConsultaHttpTest {

    private static final String STREAM = "/api/v1/usuarios/me/notificaciones/stream";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token() {
        return jwtTokenService.generateToken(9_101L, "sse@styleradar.co", "COMPRADOR");
    }

    @Test
    @DisplayName("El stream acepta un token valido en access_token")
    void streamAceptaElTokenEnLaConsulta() throws Exception {
        mockMvc.perform(get(STREAM).param("access_token", token())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token invalido, vacio o ausente en la consulta responde 401")
    void tokenInvalidoEnLaConsultaResponde401() throws Exception {
        mockMvc.perform(get(STREAM).param("access_token", "no-es-un-jwt")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM).param("access_token", "")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La cabecera Authorization manda sobre la consulta")
    void laCabeceraManda() throws Exception {
        mockMvc.perform(get(STREAM).header("Authorization", "Bearer " + token()).param("access_token", "basura"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Los demas endpoints no aceptan el token en la consulta")
    void otrosEndpointsNoLoAceptan() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones").param("access_token", token()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/me/alertas").param("access_token", token()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/me/preferencias-avisos").param("access_token", token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Solo el GET del stream lo acepta")
    void soloElGetDelStream() throws Exception {
        mockMvc.perform(post(STREAM).param("access_token", token())).andExpect(status().isUnauthorized());
    }
}
