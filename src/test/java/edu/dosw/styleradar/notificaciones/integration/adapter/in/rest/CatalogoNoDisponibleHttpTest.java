package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoFeignClient;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoNoDisponibleHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private CatalogoFeignClient catalogoFeignClient;

    @Test
    @DisplayName("NOT-10 - Debe responder 503 y no crear la alerta cuando Catalogo no responde")
    void debeResponder503YNoCrearLaAlertaCuandoCatalogoNoResponde() throws Exception {
        String token = jwtTokenService.generateToken(7001L, "sin.catalogo@styleradar.co", "COMPRADOR");
        when(catalogoFeignClient.consultarDisponibilidad(3001L))
                .thenThrow(new IllegalStateException("catalogo caido"));

        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":3001,\"canal\":\"IN_APP\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio externo no disponible"));

        mockMvc.perform(get("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
