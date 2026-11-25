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

    private static String notificacionesUrl(long usuarioId) {
        return "/api/v1/users/" + usuarioId + "/notifications";
    }

    private static String contadorUrl(long usuarioId) {
        return notificacionesUrl(usuarioId) + "/unread/count";
    }

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

        mockMvc.perform(delete(notificacionesUrl(usuarioId) + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get(contadorUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(2));
    }

    @Test
    @DisplayName("Eliminar dos veces la misma notificacion: la segunda responde 404")
    void eliminarDosVecesResponde404() throws Exception {
        long usuarioId = 71_002L;
        crearNotificaciones(usuarioId, 1);
        long notificacionId = primeraNotificacion(usuarioId);

        mockMvc.perform(delete(notificacionesUrl(usuarioId) + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(notificacionesUrl(usuarioId) + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Una notificacion eliminada ya no se puede marcar como leida: 404")
    void laEliminadaNoSeMarcaComoLeida() throws Exception {
        long usuarioId = 71_003L;
        crearNotificaciones(usuarioId, 1);
        long notificacionId = primeraNotificacion(usuarioId);
        mockMvc.perform(delete(notificacionesUrl(usuarioId) + "/" + notificacionId).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(notificacionesUrl(usuarioId) + "/" + notificacionId + "/read")
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

        mockMvc.perform(delete(notificacionesUrl(intruso) + "/" + notificacionId).header("Authorization", bearer(intruso)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(contadorUrl(propietario)).header("Authorization", bearer(propietario)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(1));
    }

    @Test
    @DisplayName("Eliminar las leidas deja las no leidas y responde cuantas elimino")
    void eliminarLeidasConservaLasNoLeidas() throws Exception {
        long usuarioId = 71_006L;
        crearNotificaciones(usuarioId, 3);
        long notificacionId = primeraNotificacion(usuarioId);
        mockMvc.perform(patch(notificacionesUrl(usuarioId) + "/" + notificacionId + "/read")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("read", "true").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadEliminadas").value(1));

        mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Eliminar en bloque sin read=true responde 422 y no borra nada")
    void enBloqueSinReadTrueResponde422() throws Exception {
        long usuarioId = 71_007L;
        crearNotificaciones(usuarioId, 2);

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("read", "false").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Eliminar por ids borra solo las indicadas, leidas o no, y responde cuantas elimino")
    void eliminarPorIds() throws Exception {
        long usuarioId = 71_010L;
        crearNotificaciones(usuarioId, 4);
        String ids = idsDeLasPrimeras(usuarioId, 2);

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", ids).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadEliminadas").value(2));

        mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get(contadorUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(2));
    }

    @Test
    @DisplayName("Los ids de otro usuario, inexistentes o ya eliminados se ignoran sin error ni filtracion")
    void eliminarPorIdsIgnoraLosAjenos() throws Exception {
        long propietario = 71_011L;
        long usuarioId = 71_012L;
        crearNotificaciones(propietario, 1);
        crearNotificaciones(usuarioId, 1);
        long ajena = primeraNotificacion(propietario);
        long propia = primeraNotificacion(usuarioId);

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", ajena + "," + propia + ",999999999")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadEliminadas").value(1));
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", ajena + "," + propia)
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadEliminadas").value(0));

        mockMvc.perform(get(contadorUrl(propietario)).header("Authorization", bearer(propietario)))
                .andExpect(jsonPath("$.cantidadNoLeidas").value(1));
    }

    @Test
    @DisplayName("Eliminar por ids acepta tambien el parametro repetido y un solo id")
    void eliminarPorIdsParametroRepetido() throws Exception {
        long usuarioId = 71_013L;
        crearNotificaciones(usuarioId, 3);
        String[] ids = idsDeLasPrimeras(usuarioId, 3).split(",");

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", ids[0]).param("ids", ids[1])
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadEliminadas").value(2));
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", ids[2]).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.cantidadEliminadas").value(1));
    }

    @Test
    @DisplayName("Combinar ids y read, no mandar nada, mas de 100 ids o ids no positivos responde 422")
    void idsInvalidosResponden422() throws Exception {
        long usuarioId = 71_014L;
        crearNotificaciones(usuarioId, 1);
        String cienUno = java.util.stream.LongStream.rangeClosed(1, 101)
                .mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));

        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", "1").param("read", "true")
                        .header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", "").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", cienUno).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete(notificacionesUrl(usuarioId)).param("ids", "0,5").header("Authorization", bearer(usuarioId)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Un id que no es numero responde 400")
    void idNoNumericoResponde400() throws Exception {
        mockMvc.perform(delete(notificacionesUrl(71_015L)).param("ids", "abc").header("Authorization", bearer(71_015L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin token responde 401")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(delete(notificacionesUrl(71_001L) + "/1")).andExpect(status().isUnauthorized());
    }

    private void crearNotificaciones(long usuarioId, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            procesarEventosExternosUseCase.generarNotificacionDirecta(
                    usuarioId, TipoNotificacion.MENSAJE_NUEVO, "Mensaje " + i, "Hola " + i,
                    "/chat/conversaciones/" + i, null);
        }
    }

    private long primeraNotificacion(long usuarioId) throws Exception {
        String json = mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get(0).get("id").asLong();
    }

    private String idsDeLasPrimeras(long usuarioId, int cantidad) throws Exception {
        String json = mockMvc.perform(get(notificacionesUrl(usuarioId)).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            ids.add(objectMapper.readTree(json).get(i).get("id").asText());
        }
        return String.join(",", ids);
    }

    private String bearer(long usuarioId) {
        return "Bearer " + jwtTokenService.generateToken(usuarioId, "usuario" + usuarioId + "@styleradar.co", "COMPRADOR");
    }
}
