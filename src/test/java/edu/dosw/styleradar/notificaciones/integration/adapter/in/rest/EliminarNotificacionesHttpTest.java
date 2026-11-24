package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** El usuario elimina notificaciones de su bandeja (borrado logico). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Eliminar notificaciones de la bandeja")
class EliminarNotificacionesHttpTest {

    private static final String NOTIFICACIONES = "/api/v1/users/me/notifications";
    private static final String CONTADOR = NOTIFICACIONES + "/unread/count";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @Test
    @DisplayName("Eliminar una notificacion responde 204 y deja de aparecer en la lista y en el contador")
    void eliminarUnaLaQuitaDeLaBandeja() throws Exception {
        long usuarioId = 71_001L;
        crearNotificaciones(usuarioId, 3);
        long notificacionId = primeraNotificacion(usuarioId);

        mockMvc.perform(delete(NOTIFICACIONES + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(2));
    }

    @Test
    @DisplayName("Eliminar dos veces la misma notificacion: la segunda responde 404")
    void eliminarDosVecesResponde404() throws Exception {
        long usuarioId = 71_002L;
        crearNotificaciones(usuarioId, 1);
        long notificacionId = primeraNotificacion(usuarioId);

        mockMvc.perform(delete(NOTIFICACIONES + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(NOTIFICACIONES + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Una notificacion eliminada ya no se puede marcar como leida: 404")
    void laEliminadaNoSeMarcaComoLeida() throws Exception {
        long usuarioId = 71_003L;
        crearNotificaciones(usuarioId, 1);
        long notificacionId = primeraNotificacion(usuarioId);
        mockMvc.perform(delete(NOTIFICACIONES + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(NOTIFICACIONES + "/" + notificacionId + "/read")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Una notificacion de otro usuario no se puede eliminar: 403, y sigue en su bandeja")
    void laAjenaResponde403() throws Exception {
        long propietario = 71_004L;
        long intruso = 71_005L;
        crearNotificaciones(propietario, 1);
        long notificacionId = primeraNotificacion(propietario);

        mockMvc.perform(delete(NOTIFICACIONES + "/" + notificacionId).header("Authorization", bearer(intruso)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(CONTADOR).header("Authorization", bearer(propietario)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(1));
    }

    @Test
    @DisplayName("Eliminar las leidas deja las no leidas y responde cuantas elimino")
    void eliminarLeidasConservaLasNoLeidas() throws Exception {
        long usuarioId = 71_006L;
        crearNotificaciones(usuarioId, 3);
        long notificacionId = primeraNotificacion(usuarioId);
        mockMvc.perform(patch(NOTIFICACIONES + "/" + notificacionId + "/read")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete(NOTIFICACIONES).param("read", "true").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadEliminadas").value(1));

        mockMvc.perform(get(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Eliminar en bloque sin read=true responde 422 y no borra nada")
    void enBloqueSinReadTrueResponde422() throws Exception {
        long usuarioId = 71_007L;
        crearNotificaciones(usuarioId, 2);

        mockMvc.perform(delete(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete(NOTIFICACIONES).param("read", "false").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Sin token responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(delete(NOTIFICACIONES + "/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tambien funciona con las rutas en espanol")
    void rutasEnEspanol() throws Exception {
        long usuarioId = 71_008L;
        crearNotificaciones(usuarioId, 1);
        long notificacionId = primeraNotificacion(usuarioId);

        mockMvc.perform(delete("/api/v1/usuarios/me/notificaciones/" + notificacionId)
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());
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
