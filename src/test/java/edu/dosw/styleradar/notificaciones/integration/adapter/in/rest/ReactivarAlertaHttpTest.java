package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
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

/** SR-RF-32: una alerta desactivada se puede volver a activar con las mismas reglas que al crearla. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Reactivar una alerta")
class ReactivarAlertaHttpTest {

    private static final long USUARIO = 9_601L;
    private static final long BUSQUEDA = 7_601L;
    private static final String BASE = "/api/v1/users/" + USUARIO + "/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private BusquedaGuardadaCopiaRepositoryPort busquedas;

    private String token;

    @BeforeEach
    void preparar() {
        token = "Bearer " + jwtTokenService.generateToken(USUARIO, "react@styleradar.co", "COMPRADOR");
        busquedas.guardar(new BusquedaGuardadaCopia(BUSQUEDA, USUARIO, "Bomber",
                new CriterioBusquedaVO(null, "CHAQUETAS", null, null, null, null, null, null, null),
                true, LocalDateTime.now()));
    }

    private long crearAlerta() throws Exception {
        String cuerpo = mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAlerta\":\"BUSQUEDA_GUARDADA\",\"busquedaGuardadaId\":" + BUSQUEDA + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("id").asLong();
    }

    private void desactivar(long id) throws Exception {
        mockMvc.perform(patch(BASE + "/" + id + "/deactivate").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESACTIVADA"));
    }

    @Test
    @DisplayName("Una alerta desactivada vuelve a ACTIVA")
    void reactivaUnaAlertaDesactivada() throws Exception {
        long id = crearAlerta();
        desactivar(id);

        mockMvc.perform(patch(BASE + "/" + id + "/activate").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    @DisplayName("Activar una alerta ya activa es idempotente y no cambia nada")
    void idempotenciaAlReactivar() throws Exception {
        long id = crearAlerta();

        mockMvc.perform(patch(BASE + "/" + id + "/activate").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    @DisplayName("Si ya hay otra alerta activa igual responde 409")
    void conflictoConOtraActiva() throws Exception {
        long primera = crearAlerta();
        desactivar(primera);
        crearAlerta();

        mockMvc.perform(patch(BASE + "/" + primera + "/activate").header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Si la busqueda guardada ya no esta activa responde 422")
    void busquedaInactiva() throws Exception {
        long id = crearAlerta();
        desactivar(id);
        busquedas.guardar(new BusquedaGuardadaCopia(BUSQUEDA, USUARIO, "Bomber",
                new CriterioBusquedaVO(null, "CHAQUETAS", null, null, null, null, null, null, null),
                false, LocalDateTime.now()));

        mockMvc.perform(patch(BASE + "/" + id + "/activate").header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("La alerta de otro usuario responde 403 y una inexistente 404 sin mostrar el id")
    void ajenaEInexistente() throws Exception {
        long id = crearAlerta();
        String otro = "Bearer " + jwtTokenService.generateToken(9_602L, "otro@styleradar.co", "COMPRADOR");

        mockMvc.perform(patch("/api/v1/users/9602/alerts/" + id + "/activate").header("Authorization", otro))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/987654/activate").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No se encontro la alerta"));
    }

    @Test
    @DisplayName("La lista y las respuestas de la alerta traen el nombre de la busqueda guardada")
    void traeElNombreDeLaBusqueda() throws Exception {
        long id = crearAlerta();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BASE).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].nombreBusqueda").value("Bomber"));
        mockMvc.perform(patch(BASE + "/" + id + "/deactivate").header("Authorization", token))
                .andExpect(jsonPath("$.nombreBusqueda").value("Bomber"));
    }

    @Test
    @DisplayName("Sin token responde 401")
    void sinToken() throws Exception {
        mockMvc.perform(patch(BASE + "/1/activate")).andExpect(status().isUnauthorized());
    }
}
