package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail;

import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EnvioCorreoPort;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envia los correos con JavaMailSender, fuera del hilo que genera la notificacion. Si el servidor de
 * correo falla se reintenta con espera creciente; agotados los intentos el correo se guarda como pendiente
 * (tabla correos_pendientes) y {@code ReenviarCorreosPendientesUseCase} lo reintenta, tambien despues de un
 * reinicio. Si no hay servidor configurado (sin spring.mail.host) los correos se omiten con un aviso en el log.
 */
@Slf4j
public class CorreoSmtpAdapter implements CorreoPort, EnvioCorreoPort {

    /** Intentos que se darian a un correo pendiente cuando no se indica otro valor. */
    public static final int MAX_INTENTOS_PENDIENTE_POR_DEFECTO = 3;

    private final JavaMailSender mailSender;
    private final String remitente;
    private final int maxIntentos;
    private final long esperaInicialMs;
    private final Executor executor;
    private final CorreoPendienteRepositoryPort pendientes;
    private final int maxIntentosPendiente;

    public CorreoSmtpAdapter(
            JavaMailSender mailSender,
            String remitente,
            int maxIntentos,
            long esperaInicialMs,
            Executor executor
    ) {
        this(mailSender, remitente, maxIntentos, esperaInicialMs, executor, null, MAX_INTENTOS_PENDIENTE_POR_DEFECTO);
    }

    public CorreoSmtpAdapter(
            JavaMailSender mailSender,
            String remitente,
            int maxIntentos,
            long esperaInicialMs,
            Executor executor,
            CorreoPendienteRepositoryPort pendientes,
            int maxIntentosPendiente
    ) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.maxIntentos = Math.max(1, maxIntentos);
        this.esperaInicialMs = Math.max(0, esperaInicialMs);
        this.executor = executor;
        this.pendientes = pendientes;
        this.maxIntentosPendiente = Math.max(1, maxIntentosPendiente);
    }

    @Override
    public void enviar(Long usuarioId, String destinatario, String asunto, String cuerpo) {
        if (mailSender == null) {
            log.warn("Correo omitido: no hay servidor SMTP configurado (spring.mail.host)");
            return;
        }
        try {
            executor.execute(() -> enviarConReintentos(usuarioId, destinatario, asunto, cuerpo));
        } catch (RuntimeException ex) {
            log.error("No se pudo programar el envio de correo a {}", destinatario, ex);
            guardarComoPendiente(usuarioId, destinatario, asunto, cuerpo, ex.getMessage());
        }
    }

    @Override
    public boolean estaDisponible() {
        return mailSender != null;
    }

    @Override
    public void enviarSincrono(String destinatario, String asunto, String cuerpo) {
        if (mailSender == null) {
            throw new ServicioExternoNoDisponibleException("No hay servidor SMTP configurado (spring.mail.host)");
        }
        try {
            mailSender.send(construirMensaje(destinatario, asunto, cuerpo));
        } catch (RuntimeException ex) {
            throw new ServicioExternoNoDisponibleException("El servidor de correo no acepto el mensaje: " + ex.getMessage(), ex);
        }
    }

    private SimpleMailMessage construirMensaje(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        return mensaje;
    }

    private void enviarConReintentos(Long usuarioId, String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = construirMensaje(destinatario, asunto, cuerpo);

        long espera = esperaInicialMs;
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                mailSender.send(mensaje);
                return;
            } catch (RuntimeException ex) {
                log.warn("Fallo el envio de correo a {} (intento {} de {}): {}", destinatario, intento, maxIntentos, ex.getMessage());
                if (intento == maxIntentos) {
                    log.error("Correo a {} sin enviar tras {} intentos, se guarda para reintentarlo", destinatario, maxIntentos);
                    guardarComoPendiente(usuarioId, destinatario, asunto, cuerpo, ex.getMessage());
                    return;
                }
                if (!dormir(espera)) {
                    guardarComoPendiente(usuarioId, destinatario, asunto, cuerpo, "interrumpido mientras reintentaba");
                    return;
                }
                espera = espera * 2;
            }
        }
    }

    /** Guarda el correo para que se reintente despues; si tampoco se puede guardar, solo queda el registro. */
    private void guardarComoPendiente(Long usuarioId, String destinatario, String asunto, String cuerpo, String error) {
        if (pendientes == null) {
            log.error("Correo a {} descartado: no hay donde guardarlo para reintentarlo", destinatario);
            return;
        }
        try {
            CorreoPendiente guardado = pendientes.guardar(
                    CorreoPendiente.nuevo(usuarioId, destinatario, asunto, cuerpo, maxIntentosPendiente, error));
            log.warn("Correo a {} guardado como pendiente {} para reintentarlo", destinatario, guardado.getId());
        } catch (RuntimeException ex) {
            log.error("Correo a {} perdido: no se pudo guardar como pendiente", destinatario, ex);
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
