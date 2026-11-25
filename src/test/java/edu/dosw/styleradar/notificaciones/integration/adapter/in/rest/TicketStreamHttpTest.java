package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El EventSource del navegador no puede enviar la cabecera Authorization. El stream SSE se abre con un
 * ticket de vida corta en {@code ?ticket=}: un JWT de sesion en la URL no se acepta y el ticket no abre
 * ningun otro endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ticket de vida corta para el stream SSE")
class TicketStreamHttpTest {

    private static final String BASE = "/api/v1/users/9201/notifications";
    private static final String TICKET = BASE + "/stream-ticket";
    private static final String STREAM = BASE + "/stream";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private String sesion() {
        return jwtTokenService.generateToken(9_201L, "ticket@styleradar.co", "COMPRADOR");
    }

    private String pedirTicket() throws Exception {
        String cuerpo = mockMvc.perform(post(TICKET).header("Authorization", "Bearer " + sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").isNotEmpty())
                .andExpect(jsonPath("$.expiraEnSegundos").value(60))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("ticket").asText();
    }

    @Test
    @DisplayName("Con un JWT de sesion se emite un ticket y el ticket abre el stream")
    void elTicketAbreElStream() throws Exception {
        mockMvc.perform(get(STREAM).param("ticket", pedirTicket())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Pedir el ticket sin JWT responde 401")
    void sinJwtNoHayTicket() throws Exception {
        mockMvc.perform(post(TICKET)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un ticket no sirve para pedir otro ticket")
    void unTicketNoPideOtroTicket() throws Exception {
        mockMvc.perform(post(TICKET).header("Authorization", "Bearer " + pedirTicket()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(TICKET).param("ticket", pedirTicket())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un JWT de sesion en ?ticket= o en ?access_token= se rechaza")
    void elJwtDeSesionEnLaUrlSeRechaza() throws Exception {
        mockMvc.perform(get(STREAM).param("ticket", sesion())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM).param("access_token", sesion())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un ticket vacio, basura o ausente responde 401")
    void ticketInvalidoResponde401() throws Exception {
        mockMvc.perform(get(STREAM).param("ticket", "no-es-un-jwt")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM).param("ticket", "")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El ticket en la cabecera Authorization no sirve")
    void elTicketEnLaCabeceraNoSirve() throws Exception {
        mockMvc.perform(get(STREAM).header("Authorization", "Bearer " + pedirTicket()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El ticket no abre ningun otro endpoint")
    void elTicketSoloAbreElStream() throws Exception {
        String ticket = pedirTicket();
        mockMvc.perform(get("/api/v1/users/9201/notifications").param("ticket", ticket))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/9201/alerts").param("ticket", ticket)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/9201/notification-preferences").param("ticket", ticket))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un ticket vencido responde 401")
    void unTicketVencidoResponde401() throws Exception {
        String vencido = jwtTokenServiceConTicketVencido().generarTicketStream(9_201L, "t@styleradar.co", List.of("COMPRADOR"));
        mockMvc.perform(get(STREAM).param("ticket", vencido)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pedir el ticket de otro usuario responde 403")
    void noSePideElTicketDeOtroUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/users/777/notifications/stream-ticket").header("Authorization", "Bearer " + sesion()))
                .andExpect(status().isForbidden());
    }

    private JwtTokenService jwtTokenServiceConTicketVencido() {
        JwtTokenService servicio = new JwtTokenService("clave_exclusiva_de_pruebas_automatizadas_0123456789", 3_600_000L);
        org.springframework.test.util.ReflectionTestUtils.setField(servicio, "ticketTtlSegundos", -30L);
        return servicio;
    }
}
