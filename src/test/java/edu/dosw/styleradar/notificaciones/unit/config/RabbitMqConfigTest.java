package edu.dosw.styleradar.notificaciones.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitMqConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.test.util.ReflectionTestUtils;

class RabbitMqConfigTest {

    @Test
    @DisplayName("NOT-08 - La cola de prendas retiradas es durable, enruta por su routing key y falla hacia la DLQ")
    void colaDePrendasRetiradasEstaEnlazadaYConDlq() {
        RabbitMqConfig config = new RabbitMqConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "styleradar.exchange");
        ReflectionTestUtils.setField(config, "dlxName", "styleradar.dlx");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "styleradar.notificaciones.error");
        ReflectionTestUtils.setField(config, "garmentDeletedQueueName", "styleradar.notificaciones.prenda-retirada.q");
        ReflectionTestUtils.setField(config, "garmentDeletedKey", "styleradar.catalogo.prenda.retirada");

        Queue cola = config.garmentDeletedQueue();
        Binding enlace = config.garmentDeletedBinding(cola, config.styleradarExchange());

        assertThat(cola.getName()).isEqualTo("styleradar.notificaciones.prenda-retirada.q");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "styleradar.dlx")
                .containsEntry("x-dead-letter-routing-key", "styleradar.notificaciones.error");
        assertThat(enlace.getExchange()).isEqualTo("styleradar.exchange");
        assertThat(enlace.getRoutingKey()).isEqualTo("styleradar.catalogo.prenda.retirada");
        assertThat(enlace.getDestination()).isEqualTo("styleradar.notificaciones.prenda-retirada.q");
    }
}
