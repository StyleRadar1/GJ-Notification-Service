package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Estado inicial del boton «Avisarme cuando llegue»: GET /alerts/check. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Comprobar si ya hay una alerta para una prenda")
class ComprobarAlertaHttpTest {

    private static final long USUARIO = 9_801L;
    private static final String ITEM = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String BASE = "/api/v1/users/me/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertaRepositoryPort alertas;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private long alertaId;

    @BeforeEach
    void preparar() {
        token = "Bearer " + jwtTokenService.generateToken(USUARIO, "check@styleradar.co", "COMPRADOR");
        alertaId = alertas.guardar(new Alerta(null, USUARIO, TipoAlerta.ITEM_AGOTADO, ITEM, "M", null,
                CanalNotificacion.AMBOS, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now())).getId();
    }

    @Test
    @DisplayName("Con una alerta activa para la prenda y talla responde suscrita=true con su id")
    void suscrita() throws Exception {
        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM).param("tallaObjetivo", "m").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscrita").value(true))
                .andExpect(jsonPath("$.alertaId").value(alertaId))
                .andExpect(jsonPath("$.tallaObjetivo").value("M"))
                .andExpect(jsonPath("$.canal").value("AMBOS"));
    }

    @Test
    @DisplayName("Sin alerta para esa talla, o sin talla, responde suscrita=false")
    void noSuscrita() throws Exception {
        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM).param("tallaObjetivo", "XL").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscrita").value(false))
                .andExpect(jsonPath("$.alertaId").doesNotExist());
        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscrita").value(false));
        mockMvc.perform(get(BASE + "/check").param("itemId", "otra-prenda").param("tallaObjetivo", "M").header("Authorization", token))
                .andExpect(jsonPath("$.suscrita").value(false));
    }

    @Test
    @DisplayName("Una alerta desactivada ya no cuenta como suscrita")
    void desactivadaNoCuenta() throws Exception {
        mockMvc.perform(patch(BASE + "/" + alertaId + "/deactivate").header("Authorization", token)).andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM).param("tallaObjetivo", "M").header("Authorization", token))
                .andExpect(jsonPath("$.suscrita").value(false));
    }

    @Test
    @DisplayName("Solo ve sus propias alertas: otro usuario responde suscrita=false")
    void soloLasPropias() throws Exception {
        String otro = "Bearer " + jwtTokenService.generateToken(9_802L, "otro@styleradar.co", "COMPRADOR");

        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM).param("tallaObjetivo", "M").header("Authorization", otro))
                .andExpect(jsonPath("$.suscrita").value(false));
    }

    @Test
    @DisplayName("Falta el itemId: 400 con el campo en errors; sin token: 401; id ajeno: 403")
    void validaciones() throws Exception {
        mockMvc.perform(get(BASE + "/check").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.itemId").exists());
        mockMvc.perform(get(BASE + "/check").param("itemId", ITEM)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/777/alerts/check").param("itemId", ITEM).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El canal AMBOS se acepta al crear una alerta de busqueda y se devuelve")
    void canalAmbosAlCrear() throws Exception {
        mockMvc.perform(post(BASE).header("Authorization", token).contentType("application/json")
                        .content("{\"tipoAlerta\":\"BUSQUEDA_GUARDADA\",\"busquedaGuardadaId\":99999,\"canal\":\"AMBOS\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post(BASE).header("Authorization", token).contentType("application/json")
                        .content("{\"tipoAlerta\":\"BUSQUEDA_GUARDADA\",\"busquedaGuardadaId\":99999,\"canal\":\"NINGUNO\"}"))
                .andExpect(status().isBadRequest());
    }
}
