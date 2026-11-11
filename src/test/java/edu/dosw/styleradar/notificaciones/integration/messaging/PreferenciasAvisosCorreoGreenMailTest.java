package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * GJ-64 - Preferencias de canal y envio por correo contra un servidor SMTP de prueba (GreenMail),
 * por la API real y con los casos de aceptacion de la tarea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "application.mail.from=no-reply@styleradar.co",
        "application.mail.retry.initial-interval-ms=50"
})
@DisplayName("GJ-64 - Preferencias de avisos y correo con GreenMail")
class PreferenciasAvisosCorreoGreenMailTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventos;

    @Autowired
    private EstadoCuentaCopiaRepositoryPort cuentas;

    @Test
    @DisplayName("Con correo y plataforma elegido llega el correo ademas del aviso en la plataforma")
    void conCorreoYPlataformaLlegaElCorreo() throws Exception {
        long usuario = 6_401L;
        registrar(usuario, "u6401@styleradar.co");
        elegir(usuario, "MENSAJE_NUEVO", "CORREO_Y_PLATAFORMA");

        procesarEventos.generarNotificacionDirecta(
                usuario, TipoNotificacion.MENSAJE_NUEVO, "Mensaje nuevo de Ana", "Hola, como estas", "/chat/1", null);

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage correo = greenMail.getReceivedMessages()[0];
        assertThat(correo.getAllRecipients()[0].toString()).isEqualTo("u6401@styleradar.co");
        assertThat(correo.getFrom()[0].toString()).isEqualTo("no-reply@styleradar.co");
        assertThat(correo.getSubject()).isEqualTo("StyleRadar: Mensaje nuevo de Ana");
        assertThat(GreenMailUtil.getBody(correo)).contains("Hola, como estas");
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Mensaje nuevo de Ana"));
    }

    @Test
    @DisplayName("Con solo plataforma elegido no llega correo, pero el aviso si se crea")
    void soloPlataformaNoLlegaCorreo() throws Exception {
        long usuario = 6_402L;
        registrar(usuario, "u6402@styleradar.co");
        elegir(usuario, "MENSAJE_NUEVO", "SOLO_PLATAFORMA");

        procesarEventos.generarNotificacionDirecta(
                usuario, TipoNotificacion.MENSAJE_NUEVO, "Solo campana", "Texto", "/chat/2", null);

        assertThat(greenMail.waitForIncomingEmail(1500, 1)).isFalse();
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones").header("Authorization", bearer(usuario)))
                .andExpect(jsonPath("$[0].titulo").value("Solo campana"));
    }

    @Test
    @DisplayName("Un usuario sin preferencias guardadas recibe solo plataforma")
    void sinPreferenciasNoLlegaCorreo() throws Exception {
        long usuario = 6_403L;
        registrar(usuario, "u6403@styleradar.co");

        procesarEventos.generarNotificacionDirecta(
                usuario, TipoNotificacion.MENSAJE_NUEVO, "Sin preferencias", "Texto", "/chat/3", null);

        assertThat(greenMail.waitForIncomingEmail(1500, 1)).isFalse();
    }

    @Test
    @DisplayName("La preferencia es por tipo: elegir correo para un tipo no activa el correo de otro")
    void laPreferenciaEsPorTipo() throws Exception {
        long usuario = 6_404L;
        registrar(usuario, "u6404@styleradar.co");
        elegir(usuario, "PAGO_CONFIRMADO", "CORREO_Y_PLATAFORMA");

        procesarEventos.generarNotificacionDirecta(
                usuario, TipoNotificacion.MENSAJE_NUEVO, "Otro tipo", "Texto", "/chat/4", null);

        assertThat(greenMail.waitForIncomingEmail(1500, 1)).isFalse();
    }

    @Test
    @DisplayName("GET lista un canal por tipo y refleja lo elegido; el resto sale como solo plataforma")
    void listarPreferencias() throws Exception {
        long usuario = 6_405L;
        elegir(usuario, "REPOSICION_STOCK", "CORREO_Y_PLATAFORMA");

        mockMvc.perform(get("/api/v1/usuarios/me/preferencias-avisos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(TipoNotificacion.values().length))
                .andExpect(jsonPath("$[?(@.tipo=='REPOSICION_STOCK')].canal").value("CORREO_Y_PLATAFORMA"))
                .andExpect(jsonPath("$[?(@.tipo=='MENSAJE_NUEVO')].canal").value("SOLO_PLATAFORMA"));
    }

    @Test
    @DisplayName("Cambiar la preferencia de un tipo la actualiza sin duplicarla")
    void cambiarLaPreferencia() throws Exception {
        long usuario = 6_406L;
        elegir(usuario, "SISTEMA", "CORREO_Y_PLATAFORMA");
        elegir(usuario, "SISTEMA", "SOLO_PLATAFORMA");

        mockMvc.perform(get("/api/v1/usuarios/me/preferencias-avisos").header("Authorization", bearer(usuario)))
                .andExpect(jsonPath("$.length()").value(TipoNotificacion.values().length))
                .andExpect(jsonPath("$[?(@.tipo=='SISTEMA')].canal").value("SOLO_PLATAFORMA"));
    }

    @Test
    @DisplayName("Un canal o un tipo invalido responde 400 y sin JWT responde 401")
    void validacionesYSeguridad() throws Exception {
        long usuario = 6_407L;
        mockMvc.perform(put("/api/v1/usuarios/me/preferencias-avisos/MENSAJE_NUEVO")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"PALOMA_MENSAJERA\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/usuarios/me/preferencias-avisos/MENSAJE_NUEVO")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/usuarios/me/preferencias-avisos/NO_EXISTE")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"SOLO_PLATAFORMA\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/usuarios/me/preferencias-avisos")).andExpect(status().isUnauthorized());
    }

    private void registrar(long usuarioId, String email) {
        cuentas.guardar(new EstadoCuentaCopia(usuarioId, email, EstadoCuenta.ACTIVO, LocalDateTime.now()));
    }

    private void elegir(long usuarioId, String tipo, String canal) throws Exception {
        mockMvc.perform(put("/api/v1/usuarios/me/preferencias-avisos/" + tipo)
                        .header("Authorization", bearer(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canal\":\"" + canal + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value(canal));
    }

    private String bearer(long usuarioId) {
        return "Bearer " + jwtTokenService.generateToken(usuarioId, "usuario" + usuarioId + "@styleradar.co", "COMPRADOR");
    }
}
