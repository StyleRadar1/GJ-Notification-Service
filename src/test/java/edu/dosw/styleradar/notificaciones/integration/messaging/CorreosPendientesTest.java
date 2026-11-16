package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.usecase.ReenviarCorreosPendientesUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EnvioCorreoPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.scheduler.CorreoPendienteRecuperador;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail.CorreoSmtpAdapter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * GJ-119 - Un correo que no se pudo enviar queda guardado en la base y se reintenta, tambien al volver a
 * levantar el servicio. Usa la base real de pruebas (H2 con las migraciones de Flyway).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GJ-119 - Correos pendientes que sobreviven a un reinicio")
class CorreosPendientesTest {

    @Autowired
    private CorreoPendienteRepositoryPort pendientes;

    private CorreoPendiente guardarPendiente(String destinatario, int maxIntentos) {
        return pendientes.guardar(CorreoPendiente.nuevo(8_001L, destinatario, "Asunto " + destinatario,
                "Cuerpo de " + destinatario, maxIntentos, "smtp caido"));
    }

    private CorreoPendiente recargar(CorreoPendiente correo) {
        return pendientes.buscarPorId(correo.getId()).orElseThrow();
    }

    private CorreoPendienteRecuperador recuperadorCon(EnvioCorreoPort envio) {
        return new CorreoPendienteRecuperador(new ReenviarCorreosPendientesUseCaseImpl(pendientes, envio));
    }

    @Test
    @DisplayName("Con el servidor de correo caido, el correo queda guardado en la base como PENDIENTE")
    void servidorCaidoDejaElCorreoGuardado() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(
                sender, "no-reply@styleradar.co", 3, 0, Runnable::run, pendientes, 3);

        adapter.enviar(8_002L, "caido-8002@styleradar.co", "Pago confirmado", "Tu plan esta activo");

        CorreoPendiente guardado = pendientes.listarPendientes(500).stream()
                .filter(c -> "caido-8002@styleradar.co".equals(c.getDestinatario()))
                .findFirst().orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(guardado.getUsuarioId()).isEqualTo(8_002L);
        assertThat(guardado.getAsunto()).isEqualTo("Pago confirmado");
        assertThat(guardado.getCuerpo()).isEqualTo("Tu plan esta activo");
        assertThat(guardado.getIntentos()).isZero();
        assertThat(guardado.getMaxIntentos()).isEqualTo(3);
        assertThat(guardado.getUltimoError()).contains("smtp caido");
    }

    @Test
    @DisplayName("Al levantar el servicio los correos que estaban en la base se envian y quedan ENVIADO")
    void alArrancarSeEnvianLosPendientes() {
        CorreoPendiente a = guardarPendiente("arranque-a@styleradar.co", 3);
        CorreoPendiente b = guardarPendiente("arranque-b@styleradar.co", 3);
        EnvioCorreoPort envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenReturn(true);

        recuperadorCon(envio).alArrancar();

        assertThat(recargar(a).getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(recargar(b).getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(pendientes.listarPendientes(500)).extracting(CorreoPendiente::getId)
                .doesNotContain(a.getId(), b.getId());
    }

    @Test
    @DisplayName("Si el servidor sigue caido se cuenta cada intento y al llegar al maximo el correo queda FALLIDO")
    void sigueCaidoHastaQuedarFallido() {
        CorreoPendiente correo = guardarPendiente("fallido-8003@styleradar.co", 2);
        EnvioCorreoPort envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenReturn(true);
        doThrow(new ServicioExternoNoDisponibleException("sigue caido")).when(envio).enviarSincrono(any(), any(), any());
        CorreoPendienteRecuperador recuperador = recuperadorCon(envio);

        recuperador.periodicamente();
        CorreoPendiente trasUno = recargar(correo);
        assertThat(trasUno.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(trasUno.getIntentos()).isEqualTo(1);
        assertThat(trasUno.getUltimoError()).contains("sigue caido");

        recuperador.periodicamente();
        CorreoPendiente trasDos = recargar(correo);
        assertThat(trasDos.getEstado()).isEqualTo(EstadoCorreo.FALLIDO);
        assertThat(trasDos.getIntentos()).isEqualTo(2);

        // ya no se reintenta: una tercera pasada no cambia nada
        recuperador.periodicamente();
        assertThat(recargar(correo).getIntentos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un correo que falla una vez y despues sale queda ENVIADO con su intento contado")
    void fallaUnaVezYLuegoSale() {
        CorreoPendiente correo = guardarPendiente("luego-8004@styleradar.co", 3);
        EnvioCorreoPort envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenReturn(true);
        doThrow(new ServicioExternoNoDisponibleException("caido")).doNothing().when(envio)
                .enviarSincrono("luego-8004@styleradar.co", "Asunto luego-8004@styleradar.co", "Cuerpo de luego-8004@styleradar.co");
        CorreoPendienteRecuperador recuperador = recuperadorCon(envio);

        recuperador.periodicamente();
        assertThat(recargar(correo).getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        recuperador.periodicamente();

        CorreoPendiente resultado = recargar(correo);
        assertThat(resultado.getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(resultado.getIntentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sin servidor de correo configurado los pendientes no se tocan ni gastan intentos")
    void sinServidorLosPendientesSeConservan() {
        CorreoPendiente correo = guardarPendiente("sin-servidor-8005@styleradar.co", 3);
        EnvioCorreoPort envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenReturn(false);

        recuperadorCon(envio).alArrancar();

        CorreoPendiente igual = recargar(correo);
        assertThat(igual.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(igual.getIntentos()).isZero();
    }

    @Test
    @DisplayName("Los pendientes se listan del mas antiguo al mas nuevo y sin los enviados ni los fallidos")
    void listaSoloLosReintentables() {
        CorreoPendiente primero = guardarPendiente("orden-1@styleradar.co", 3);
        CorreoPendiente enviado = guardarPendiente("orden-2@styleradar.co", 3);
        enviado.marcarEnviado();
        pendientes.guardar(enviado);
        CorreoPendiente fallido = guardarPendiente("orden-3@styleradar.co", 1);
        fallido.registrarFallo("sin remedio");
        pendientes.guardar(fallido);
        CorreoPendiente ultimo = guardarPendiente("orden-4@styleradar.co", 3);

        List<Long> ids = pendientes.listarPendientes(500).stream().map(CorreoPendiente::getId).toList();

        assertThat(ids).contains(primero.getId(), ultimo.getId()).doesNotContain(enviado.getId(), fallido.getId());
        assertThat(ids.indexOf(primero.getId())).isLessThan(ids.indexOf(ultimo.getId()));
        assertThat(pendientes.listarPendientes(1)).hasSize(1);
        assertThat(recargar(enviado).getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(recargar(fallido).getEstado()).isEqualTo(EstadoCorreo.FALLIDO);
    }

    @Test
    @DisplayName("Un fallo inesperado del reintento no tumba al recuperador")
    void unFalloInesperadoNoTumbaAlRecuperador() {
        EnvioCorreoPort envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenThrow(new IllegalStateException("inesperado"));

        recuperadorCon(envio).alArrancar();
        recuperadorCon(envio).periodicamente();
    }
}
