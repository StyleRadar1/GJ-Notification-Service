package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Cada error trae un {@code code} estable para que el front elija el mensaje sin leer el texto de detail. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Codigos de error estables")
class CodigosDeErrorHttpTest {

    private static final String BASE = "/api/v1/users/9901/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private EstadoCuentaCopiaRepositoryPort cuentas;

    private String token;

    @BeforeEach
    void preparar() {
        token = "Bearer " + jwtTokenService.generateToken(9_901L, "codigos@styleradar.co", "COMPRADOR");
    }

    @Test
    @DisplayName("404: ALERT_NOT_FOUND y NOTIFICATION_NOT_FOUND")
    void noEncontrados() throws Exception {
        mockMvc.perform(patch(BASE + "/987654/deactivate").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ALERT_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/users/9901/notifications/987654/read").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/ruta/que/no/existe").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }

    @Test
    @DisplayName("403: ACCESS_DENIED al pedir los datos de otro usuario")
    void accesoDenegado() throws Exception {
        mockMvc.perform(get("/api/v1/users/777/alerts").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("422: SAVED_SEARCH_NOT_AVAILABLE si la busqueda no existe y ACCOUNT_NOT_ALLOWED si la cuenta esta bloqueada")
    void reglasDeNegocio() throws Exception {
        String cuerpo = "{\"tipoAlerta\":\"BUSQUEDA_GUARDADA\",\"busquedaGuardadaId\":99999}";
        mockMvc.perform(post(BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SAVED_SEARCH_NOT_AVAILABLE"));

        cuentas.guardar(new EstadoCuentaCopia(9_901L, "codigos@styleradar.co", EstadoCuenta.BLOQUEADO, LocalDateTime.now()));
        mockMvc.perform(post(BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("400: MALFORMED_REQUEST con un JSON roto e INVALID_PARAMETER con un valor que no es un numero")
    void solicitudesInvalidas() throws Exception {
        mockMvc.perform(post(BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(patch(BASE + "/abc/deactivate").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("El filtro read activa la respuesta paginada")
    void filtroRead() throws Exception {
        mockMvc.perform(get("/api/v1/users/9901/notifications").param("read", "false").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
        mockMvc.perform(get("/api/v1/users/9901/notifications").param("read", "true").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/v1/users/9901/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
