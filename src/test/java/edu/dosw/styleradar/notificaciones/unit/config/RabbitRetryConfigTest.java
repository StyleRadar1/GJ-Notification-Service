package edu.dosw.styleradar.notificaciones.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitRetryConfig;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.retry.support.RetryTemplate;

class RabbitRetryConfigTest {

    private final RetryTemplate plantilla = RabbitRetryConfig.crearPlantillaDeReintentos(3, 1, 1.5, 5);

    @Test
    @DisplayName("NOT-11 - Un fallo transitorio se intenta 3 veces antes de rendirse")
    void falloTransitorioSeIntentaTresVeces() {
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw envuelto(new IllegalStateException("base de datos no disponible"));
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(3);
    }

    @Test
    @DisplayName("NOT-11 - Un payload invalido no se reintenta")
    void payloadInvalidoNoSeReintenta() {
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw envuelto(new AmqpRejectAndDontRequeueException("payload invalido"));
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("NOT-11 - Un mensaje ilegible no se reintenta")
    void mensajeIlegibleNoSeReintenta() {
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw envuelto(new MessageConversionException("json mal formado"));
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("NOT-11 - Un fallo que se recupera antes del tercer intento no llega a rendirse")
    void falloQueSeRecuperaNoSeRinde() throws Exception {
        AtomicInteger intentos = new AtomicInteger();

        String resultado = plantilla.execute(contexto -> {
            if (intentos.incrementAndGet() < 3) {
                throw envuelto(new IllegalStateException("fallo momentaneo"));
            }
            return "procesado";
        });

        assertThat(resultado).isEqualTo("procesado");
        assertThat(intentos).hasValue(3);
    }

    private ListenerExecutionFailedException envuelto(Throwable causa) {
        return new ListenerExecutionFailedException("fallo en el listener", causa);
    }
}
