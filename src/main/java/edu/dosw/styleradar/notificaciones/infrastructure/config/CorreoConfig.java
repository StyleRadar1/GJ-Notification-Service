package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.mail.CorreoSmtpAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class CorreoConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService correoExecutor() {
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread hilo = new Thread(runnable, "correo-avisos");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    @Bean
    public CorreoPort correoPort(
            ObjectProvider<JavaMailSender> mailSender,
            ExecutorService correoExecutor,
            @Value("${application.mail.from:no-reply@styleradar.co}") String remitente,
            @Value("${application.mail.retry.max-attempts:3}") int maxIntentos,
            @Value("${application.mail.retry.initial-interval-ms:1000}") long esperaInicialMs
    ) {
        return new CorreoSmtpAdapter(mailSender.getIfAvailable(), remitente, maxIntentos, esperaInicialMs, correoExecutor);
    }
}
