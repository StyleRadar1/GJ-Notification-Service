package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GJ-62 - La campana en tiempo real: los eventos llevan su id, al reconectarse con Last-Event-ID se
 * reenvia lo que el usuario perdio, y sin JWT valido se responde 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GJ-62 - Campana en tiempo real con SSE")
class CampanaSseHttpTest {

    private static final String STREAM = "/api/v1/usuarios/me/notificaciones/stream";
    private static final String NOTIFICACIONES = "/api/v1/usuarios/me/notificaciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @Test
    @DisplayName("Sin JWT o con un token invalido no se puede suscribir: 401")
    void sinJwtValidoResponde401() throws Exception {
        mockMvc.perform(get(STREAM)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(STREAM).header("Authorization", "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con JWT valido se abre el flujo SSE y se recibe la confirmacion de conexion")
    void conJwtValidoSeAbreElFlujo() throws Exception {
        MvcResult resultado = abrir(62_101L, null);

        assertThat(resultado.getResponse().getContentAsString()).contains("event:connected");
    }

    @Test
    @DisplayName("Una notificacion nueva le llega a la campana abierta, con su id de evento")
    void laNotificacionNuevaLlegaConSuId() throws Exception {
        long usuarioId = 62_102L;
        MvcResult resultado = abrir(usuarioId, null);

        procesarEventosExternosUseCase.generarNotificacionDirecta(
                usuarioId, TipoNotificacion.MENSAJE_NUEVO, "Mensaje nuevo", "Hola", "/chat/conversaciones/1", null);
        long notificacionId = ids(usuarioId).get(0);

        String flujo = resultado.getResponse().getContentAsString();
        assertThat(flujo).contains("id:" + notificacionId).contains("event:notificacion").contains("Mensaje nuevo");
    }

    @Test
    @DisplayName("Al reconectarse con Last-Event-ID recibe solo las notificaciones que llegaron despues")
    void alReconectarseRecibeLasPerdidas() throws Exception {
        long usuarioId = 62_103L;
        for (int i = 1; i <= 3; i++) {
            procesarEventosExternosUseCase.generarNotificacionDirecta(
                    usuarioId, TipoNotificacion.MENSAJE_NUEVO, "Aviso " + i, "Texto " + i, "/chat/" + i, null);
        }
        List<Long> ids = ids(usuarioId);
        long ultimoRecibido = ids.get(0);

        String flujo = abrir(usuarioId, String.valueOf(ultimoRecibido)).getResponse().getContentAsString();

        assertThat(flujo).doesNotContain("id:" + ultimoRecibido + "\n");
        assertThat(flujo).contains("id:" + ids.get(1)).contains("id:" + ids.get(2));
        assertThat(flujo.indexOf("id:" + ids.get(1))).isLessThan(flujo.indexOf("id:" + ids.get(2)));
    }

    @Test
    @DisplayName("Sin Last-Event-ID, o con uno que no es un numero, no se reenvia nada")
    void sinIdValidoNoSeReenviaNada() throws Exception {
        long usuarioId = 62_104L;
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                usuarioId, TipoNotificacion.MENSAJE_NUEVO, "Aviso", "Texto", "/chat/1", null);

        assertThat(abrir(usuarioId, null).getResponse().getContentAsString()).doesNotContain("event:notificacion");
        assertThat(abrir(usuarioId, "no-es-un-numero").getResponse().getContentAsString())
                .doesNotContain("event:notificacion");
    }

    @Test
    @DisplayName("Un usuario no recibe las notificaciones de otro al reconectarse")
    void noSeReenvianLasNotificacionesDeOtroUsuario() throws Exception {
        long propietario = 62_105L;
        long otro = 62_106L;
        procesarEventosExternosUseCase.generarNotificacionDirecta(
                propietario, TipoNotificacion.MENSAJE_NUEVO, "Privado", "Solo mio", "/chat/9", null);

        String flujo = abrir(otro, "0").getResponse().getContentAsString();

        assertThat(flujo).doesNotContain("Privado").doesNotContain("event:notificacion");
    }

    private MvcResult abrir(long usuarioId, String ultimoEventoId) throws Exception {
        var peticion = get(STREAM).header("Authorization", bearer(usuarioId));
        if (ultimoEventoId != null) {
            peticion = peticion.header("Last-Event-ID", ultimoEventoId);
        }
        return mockMvc.perform(peticion).andExpect(request().asyncStarted()).andReturn();
    }

    /** Ids de las notificaciones del usuario, de la mas antigua a la mas reciente. */
    private List<Long> ids(long usuarioId) throws Exception {
        String json = mockMvc.perform(get(NOTIFICACIONES).header("Authorization", bearer(usuarioId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        objectMapper.readTree(json).forEach(nodo -> ids.add(nodo.get("id").asLong()));
        ids.sort(Long::compareTo);
        return ids;
    }

    private String bearer(long usuarioId) {
        return "Bearer " + jwtTokenService.generateToken(usuarioId, "usuario" + usuarioId + "@styleradar.co", "COMPRADOR");
    }
}
