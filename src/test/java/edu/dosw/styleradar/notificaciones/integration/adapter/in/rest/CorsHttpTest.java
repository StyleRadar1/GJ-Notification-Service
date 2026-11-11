package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** El navegador del frontend en localhost:5173 puede llamar al servicio; otros origenes no. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CORS para el frontend")
class CorsHttpTest {

    private static final String RUTA = "/api/v1/usuarios/me/notificaciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("El preflight desde localhost:5173 se acepta, sin JWT, con los metodos y cabeceras que usa el frontend")
    void preflightDelFrontendSeAcepta() throws Exception {
        mockMvc.perform(options(RUTA)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "authorization,content-type,last-event-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    @DisplayName("Una llamada real desde localhost:5173 devuelve la cabecera de origen permitido")
    void laRespuestaTraeLaCabeceraCors() throws Exception {
        String token = jwtTokenService.generateToken(6_601L, "u6601@styleradar.co", "COMPRADOR");
        mockMvc.perform(get(RUTA)
                        .header("Origin", "http://localhost:5173")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("Un origen que no esta en la lista se rechaza")
    void origenNoPermitidoSeRechaza() throws Exception {
        mockMvc.perform(options(RUTA)
                        .header("Origin", "http://sitio-desconocido.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
