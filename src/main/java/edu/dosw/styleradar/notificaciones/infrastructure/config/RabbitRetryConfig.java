package edu.dosw.styleradar.notificaciones.infrastructure.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;

/**
 * Politica de reintentos de los consumidores (SR-RNF-15).
 *
 * <p>Un fallo transitorio se reintenta hasta {@code max-attempts} veces con espera exponencial y, si
 * se agotan, el mensaje se rechaza sin reencolar y llega a la cola de errores. Un mensaje invalido
 * (payload incompleto o ilegible) no se reintenta: va directo a la cola de errores.</p>
 */
@Configuration
public class RabbitRetryConfig {

    public static RetryTemplate crearPlantillaDeReintentos(
            int maxAttempts, long initialIntervalMs, double multiplier, long maxIntervalMs) {
        return RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .exponentialBackoff(initialIntervalMs, multiplier, maxIntervalMs)
                .notRetryOn(AmqpRejectAndDontRequeueException.class)
                .notRetryOn(MessageConversionException.class)
                .traversingCauses()
                .build();
    }

    @Bean
    public RetryOperationsInterceptor listenerRetryInterceptor(
            @Value("${application.messaging.retry.max-attempts:3}") int maxAttempts,
            @Value("${application.messaging.retry.initial-interval-ms:1000}") long initialIntervalMs,
            @Value("${application.messaging.retry.multiplier:2.0}") double multiplier,
            @Value("${application.messaging.retry.max-interval-ms:10000}") long maxIntervalMs
    ) {
        return RetryInterceptorBuilder.stateless()
                .retryOperations(crearPlantillaDeReintentos(maxAttempts, initialIntervalMs, multiplier, maxIntervalMs))
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public ContainerCustomizer<SimpleMessageListenerContainer> listenerContainerCustomizer(
            RetryOperationsInterceptor listenerRetryInterceptor
    ) {
        return container -> {
            container.setAdviceChain(listenerRetryInterceptor);
            container.setDefaultRequeueRejected(false);
        };
    }
}
