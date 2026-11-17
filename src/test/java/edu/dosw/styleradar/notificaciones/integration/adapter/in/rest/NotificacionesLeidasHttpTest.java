package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** GJ-60 - Una prueba HTTP por cada criterio de aceptacion de marcar como leidas y el contador. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GJ-60 - Marcar como leidas y contador de no leidas")
class NotificacionesLeidasHttpTest {

    private static final String NOTIFICACIONES = "/api/v1/usuarios/me/notificaciones";
    private static final String CONTADOR = NOTIFICACIONES + "/no-leidas/contador";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @Test
    @DisplayName("Dadas notificaciones sin leer, el contador devuelve cuantas tengo sin leer")
    void elContadorDevuelveLasNoLeidas() throws Exception {
        long usuarioId = 60_101L;
        crearNotificaciones(usuarioId, 3);

        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadNoLeidas").value(3));
    }

    @Test
    @DisplayName("Al marcar una como leida, queda leida y el contador baja en 1")
    void marcarUnaBajaElContadorEnUno() throws Exception {
        long usuarioId = 60_102L;
        crearNotificaciones(usuarioId, 3);
        long notificacionId = primeraNotificacion(usuarioId);

        mockMvc.perform(patch(NOTIFICACIONES + "/" + notificacionId + "/leer")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));

        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(2));
    }

    @Test
    @DisplayName("Con varias no leidas, marcar todas como leidas deja el contador en 0")
    void marcarTodasDejaElContadorEnCero() throws Exception {
        long usuarioId = 60_103L;
        crearNotificaciones(usuarioId, 4);

        mockMvc.perform(patch(NOTIFICACIONES + "/leer-todas").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadNoLeidas").value(0));

        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(0));
    }

    @Test
    @DisplayName("Una notificacion de otro usuario no se puede marcar: 403, y sigue sin leer")
    void laNotificacionAjenaResponde403() throws Exception {
        long propietario = 60_104L;
        long intruso = 60_105L;
        crearNotificaciones(propietario, 1);
        long notificacionId = primeraNotificacion(propietario);

        mockMvc.perform(patch(NOTIFICACIONES + "/" + notificacionId + "/leer")
                        .header("Authorization", bearer(intruso)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(propietario)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(1));
    }

    private void crearNotificaciones(long usuarioId, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            procesarEventosExternosUseCase.generarNotificacionDirecta(
                    usuarioId, TipoNotificacion.MENSAJE_NUEVO, "Mensaje " + i, "Hola " + i,
                    "/chat/conversaciones/" + i, null);
        }
    }

    private long primeraNotificacion(long usuarioId) throws Exception {
        String json = mockMvc.perform(get(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get(0).get("id").asLong();
    }

    private String bearer(long usuarioId) {
        return "Bearer " + jwtTokenService.generateToken(usuarioId, "usuario" + usuarioId + "@styleradar.co", "COMPRADOR");
    }
}
