package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail;

import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envia los correos con JavaMailSender, fuera del hilo que genera la notificacion. Si el servidor de
 * correo falla se reintenta con espera creciente; agotados los intentos se registra el error. Si no hay
 * servidor configurado (sin spring.mail.host) los correos se omiten con un aviso en el log.
 */
@Slf4j
public class CorreoSmtpAdapter implements CorreoPort {

    private final JavaMailSender mailSender;
    private final String remitente;
    private final int maxIntentos;
    private final long esperaInicialMs;
    private final Executor executor;

    public CorreoSmtpAdapter(
            JavaMailSender mailSender,
            String remitente,
            int maxIntentos,
            long esperaInicialMs,
            Executor executor
    ) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.maxIntentos = Math.max(1, maxIntentos);
        this.esperaInicialMs = Math.max(0, esperaInicialMs);
        this.executor = executor;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        if (mailSender == null) {
            log.warn("Correo omitido: no hay servidor SMTP configurado (spring.mail.host)");
            return;
        }
        try {
            executor.execute(() -> enviarConReintentos(destinatario, asunto, cuerpo));
        } catch (RuntimeException ex) {
            log.error("No se pudo programar el envio de correo a {}", destinatario, ex);
        }
    }

    private void enviarConReintentos(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        long espera = esperaInicialMs;
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                mailSender.send(mensaje);
                return;
            } catch (RuntimeException ex) {
                log.warn("Fallo el envio de correo a {} (intento {} de {}): {}", destinatario, intento, maxIntentos, ex.getMessage());
                if (intento == maxIntentos) {
                    log.error("Correo a {} descartado tras {} intentos", destinatario, maxIntentos);
                    return;
                }
                if (!dormir(espera)) {
                    return;
                }
                espera = espera * 2;
            }
        }
    }

    private boolean dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
