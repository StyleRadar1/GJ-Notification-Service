package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail.CorreoSmtpAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        adapter.enviar("ana@styleradar.co", "Asunto", "Cuerpo");

        verify(sender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Agotados los intentos se descarta el correo sin lanzar excepcion")
    void agotaLosIntentosSinFallar() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run);

        assertThatCode(() -> adapter.enviar("ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();

        verify(sender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("El primer intento exitoso no se repite")
    void unSoloEnvioSiFunciona() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doNothing().when(sender).send(any(SimpleMailMessage.class));
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0, Runnable::run);

        adapter.enviar("ana@styleradar.co", "Asunto", "Cuerpo");

        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Sin servidor SMTP configurado el correo se omite sin error")
    void sinServidorSeOmite() {
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(null, "no-reply@styleradar.co", 3, 0, Runnable::run);

        assertThatCode(() -> adapter.enviar("ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Si no se puede programar el envio tampoco se propaga el error")
    void falloDelExecutorNoSePropaga() {
        JavaMailSender sender = mock(JavaMailSender.class);
        CorreoSmtpAdapter adapter = new CorreoSmtpAdapter(sender, "no-reply@styleradar.co", 3, 0,
                tarea -> { throw new java.util.concurrent.RejectedExecutionException("lleno"); });

        assertThatCode(() -> adapter.enviar("ana@styleradar.co", "Asunto", "Cuerpo")).doesNotThrowAnyException();

        verify(sender, never()).send(any(SimpleMailMessage.class));
    }
}
