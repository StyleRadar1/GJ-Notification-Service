package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail.CorreoSmtpAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CorreoConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService correoExecutor() {
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread hilo = new Thread(runnable, "correo-avisos");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    /** Un solo adaptador atiende el envio asincrono (CorreoPort) y el sincrono de los reintentos (EnvioCorreoPort). */
    @Bean
    public CorreoSmtpAdapter correoSmtpAdapter(
            ObjectProvider<JavaMailSender> mailSender,
            ExecutorService correoExecutor,
            CorreoPendienteRepositoryPort correoPendienteRepositoryPort,
            @Value("${application.mail.from:no-reply@styleradar.co}") String remitente,
            @Value("${application.mail.retry.max-attempts:3}") int maxIntentos,
            @Value("${application.mail.retry.initial-interval-ms:1000}") long esperaInicialMs,
            @Value("${application.mail.pending.max-attempts:3}") int maxIntentosPendiente
    ) {
        return new CorreoSmtpAdapter(mailSender.getIfAvailable(), remitente, maxIntentos, esperaInicialMs,
                correoExecutor, correoPendienteRepositoryPort, maxIntentosPendiente);
    }
}
