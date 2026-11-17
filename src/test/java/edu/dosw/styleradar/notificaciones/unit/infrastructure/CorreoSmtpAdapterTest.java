package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail.CorreoSmtpAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** GJ-64 - El correo se reintenta si el servidor falla y un fallo nunca llega a quien genera el aviso. */
@DisplayName("GJ-64 - Adaptador de correo: reintentos")
class CorreoSmtpAdapterTest {

    @Test
    @DisplayName("Si el servidor falla una vez, se reintenta y el correo sale en el segundo intento")
    void reintentaYSale() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).doNothing().when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run);

        adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verify(sender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Agotados los intentos se descarta el correo sin lanzar excepcion")
    void agotaLosIntentosSinFallar() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run);

        assertThatCode(() -> adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();

        verify(sender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("El primer intento exitoso no se repite")
    void unSoloEnvioSiFunciona() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doNothing().when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run);

        adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Sin servidor SMTP configurado el correo se omite sin error")
    void sinServidorSeOmite() {
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(null, "no-reply@styleradar.co", 3, 0, Runnable::run);

        assertThatCode(() -> adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Si no se puede programar el envio tampoco se propaga el error")
    void falloDelExecutorNoSePropaga() {
        JavaMailSender sender = mock(JavaMailSender.class);
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0,
                tarea -> { throw new java.util.concurrent.RejectedExecutionException("lleno"); });

        assertThatCode(() -> adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    private static CorreoSmtpAdapter conPendientes(JavaMailSender sender, CorreoPendienteRepositoryPort pendientes) {
        return new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run, pendientes, 3);
    }

    @Test
    @DisplayName("GJ-119 - Con el servidor caido, agotados los intentos el correo queda guardado como PENDIENTE")
    void servidorCaidoDejaElCorreoPendiente() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoPendienteRepositoryPort pendientes = mock(CorreoPendienteRepositoryPort.class);
        when(pendientes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        conPendientes(sender, pendientes).enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verify(sender, times(3)).send(any(SimpleMailMessage.class));
        ArgumentCaptor<CorreoPendiente> captor = ArgumentCaptor.forClass(CorreoPendiente.class);
        verify(pendientes).guardar(captor.capture());
        CorreoPendiente guardado = captor.getValue();
        assertThat(guardado.getUsuarioId()).isEqualTo(7L);
        assertThat(guardado.getDestinatario()).isEqualTo("ana@styleradar.co");
        assertThat(guardado.getAsunto()).isEqualTo("Asunto");
        assertThat(guardado.getCuerpo()).isEqualTo("Cuerpo");
        assertThat(guardado.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(guardado.getIntentos()).isZero();
        assertThat(guardado.getMaxIntentos()).isEqualTo(3);
        assertThat(guardado.getUltimoError()).contains("caido");
    }

    @Test
    @DisplayName("GJ-119 - Si el correo sale no se guarda nada")
    void correoQueSaleNoSeGuarda() {
        JavaMailSender sender = mock(JavaMailSender.class);
        CorreoPendienteRepositoryPort pendientes = mock(CorreoPendienteRepositoryPort.class);

        conPendientes(sender, pendientes).enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verifyNoInteractions(pendientes);
    }

    @Test
    @DisplayName("GJ-119 - Si ni siquiera se puede guardar el pendiente, el error no llega a quien genera el aviso")
    void fallaAlGuardarElPendienteNoSePropaga() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoPendienteRepositoryPort pendientes = mock(CorreoPendienteRepositoryPort.class);
        when(pendientes.guardar(any())).thenThrow(new IllegalStateException("base de datos caida"));

        assertThatCode(() -> conPendientes(sender, pendientes).enviar(7L, "ana@styleradar.co", "A", "C"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GJ-119 - Si no se puede programar el envio, el correo se guarda como pendiente")
    void siElExecutorRechazaElCorreoQuedaPendiente() {
        JavaMailSender sender = mock(JavaMailSender.class);
        CorreoPendienteRepositoryPort pendientes = mock(CorreoPendienteRepositoryPort.class);
        when(pendientes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0,
                tarea -> { throw new java.util.concurrent.RejectedExecutionException("lleno"); }, pendientes, 3);

        adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verify(pendientes).guardar(any(CorreoPendiente.class));
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("GJ-119 - Sin servidor SMTP no se guarda nada y el envio sincrono avisa que no esta disponible")
    void sinServidorNoGuardaNiEnvia() {
        CorreoPendienteRepositoryPort pendientes = mock(CorreoPendienteRepositoryPort.class);
        CorreoSmtpAdapter adapter = conPendientes(null, pendientes);

        adapter.enviar(7L, "ana@styleradar.co", "Asunto", "Cuerpo");

        verifyNoInteractions(pendientes);
        assertThat(adapter.estaDisponible()).isFalse();
        assertThatThrownBy(() -> adapter.enviarSincrono("ana@styleradar.co", "A", "C"))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    @Test
    @DisplayName("GJ-119 - El envio sincrono manda el mensaje y traduce el fallo del servidor")
    void envioSincronoEnviaOTraduceElFallo() {
        JavaMailSender sender = mock(JavaMailSender.class);
        CorreoSmtpAdapter adapter = conPendientes(sender, mock(CorreoPendienteRepositoryPort.class));

        adapter.enviarSincrono("ana@styleradar.co", "Asunto", "Cuerpo");
        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("ana@styleradar.co");
        assertThat(mensaje.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(mensaje.getValue().getText()).isEqualTo("Cuerpo");
        assertThat(adapter.estaDisponible()).isTrue();

        doThrow(new MailSendException("rechazado")).when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> adapter.enviarSincrono("ana@styleradar.co", "A", "C"))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .hasMessageContaining("rechazado");
    }

    @Test
    @DisplayName("GJ-119 - Sin lugar donde guardar los pendientes el correo se descarta sin lanzar excepcion")
    void sinRepositorioSeDescartaSinFallar() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> conPendientes(sender, null).enviar(7L, "ana@styleradar.co", "A", "C"))
                .doesNotThrowAnyException();
    }
}
