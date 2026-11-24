package edu.dosw.styleradar.notificaciones.unit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaRetiradaEventPayload;
import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitMqConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;

class RabbitMqConfigTest {

    @Test
    @DisplayName("GJ-117 - La cola de fundaciones desactivadas es durable, enruta por su routing key y falla hacia la DLQ")
    void colaDeFundacionesDesactivadasEstaEnlazadaYConDlq() {
        RabbitMqConfig config = new RabbitMqConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "styleradar.exchange");
        ReflectionTestUtils.setField(config, "dlxName", "styleradar.dlx");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "styleradar.notificaciones.error");
        ReflectionTestUtils.setField(config, "foundationDeactivatedQueueName",
                "styleradar.notificaciones.fundacion-desactivada.q");
        ReflectionTestUtils.setField(config, "foundationDeactivatedKey", "styleradar.tiendas.fundacion.desactivada");

        Queue cola = config.foundationDeactivatedQueue();
        Binding enlace = config.foundationDeactivatedBinding(cola, config.styleradarExchange());

        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "styleradar.dlx")
                .containsEntry("x-dead-letter-routing-key", "styleradar.notificaciones.error");
        assertThat(enlace.getExchange()).isEqualTo("styleradar.exchange");
        assertThat(enlace.getRoutingKey()).isEqualTo("styleradar.tiendas.fundacion.desactivada");
        assertThat(enlace.getDestination()).isEqualTo("styleradar.notificaciones.fundacion-desactivada.q");
    }

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

    @Test
    @DisplayName("La cola de cuentas eliminadas es durable, enruta por cuenta.eliminada y falla hacia la DLQ")
    void colaDeCuentasEliminadasEstaEnlazadaYConDlq() {
        RabbitMqConfig config = new RabbitMqConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "styleradar.exchange");
        ReflectionTestUtils.setField(config, "dlxName", "styleradar.dlx");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "styleradar.notificaciones.error");
        ReflectionTestUtils.setField(config, "accountDeletedQueueName", "styleradar.notificaciones.cuenta-eliminada.q");
        ReflectionTestUtils.setField(config, "accountDeletedKey", "styleradar.usuarios.cuenta.eliminada");

        Queue cola = config.accountDeletedQueue();
        Binding enlace = config.accountDeletedBinding(cola, config.styleradarExchange());

        assertThat(cola.getName()).isEqualTo("styleradar.notificaciones.cuenta-eliminada.q");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "styleradar.dlx")
                .containsEntry("x-dead-letter-routing-key", "styleradar.notificaciones.error");
        assertThat(enlace.getRoutingKey()).isEqualTo("styleradar.usuarios.cuenta.eliminada");
        assertThat(enlace.getDestination()).isEqualTo("styleradar.notificaciones.cuenta-eliminada.q");
    }

    @Test
    @DisplayName("CADCHE v2 - Enlaces alias user.status.changed, foundation.deactivated y cola foundation.reactivated")
    void colasYBindingsV2DeUsuariosYTiendasCadche() {
        RabbitMqConfig config = new RabbitMqConfig();
        ReflectionTestUtils.setField(config, "exchangeName", "styleradar.exchange");
        ReflectionTestUtils.setField(config, "dlxName", "styleradar.dlx");
        ReflectionTestUtils.setField(config, "dlqRoutingKey", "styleradar.notificaciones.error");
        ReflectionTestUtils.setField(config, "userStatusChangedQueueName",
                "styleradar.notificaciones.usuario-estado.q");
        ReflectionTestUtils.setField(config, "userStatusChangedAltKey", "user.status.changed");
        ReflectionTestUtils.setField(config, "foundationDeactivatedQueueName",
                "styleradar.notificaciones.fundacion-desactivada.q");
        ReflectionTestUtils.setField(config, "foundationDeactivatedAltKey", "foundation.deactivated");
        ReflectionTestUtils.setField(config, "foundationReactivatedQueueName",
                "styleradar.notificaciones.fundacion-reactivada.q");
        ReflectionTestUtils.setField(config, "foundationReactivatedKey", "styleradar.tiendas.fundacion.reactivada");
        ReflectionTestUtils.setField(config, "foundationReactivatedAltKey", "foundation.reactivated");

        TopicExchange exchange = config.styleradarExchange();
        Queue colaUsuario = config.userStatusChangedQueue();
        Binding enlaceUsuarioAlt = config.userStatusChangedAltBinding(colaUsuario, exchange);
        Queue colaFundDesact = config.foundationDeactivatedQueue();
        Binding enlaceFundDesactAlt = config.foundationDeactivatedAltBinding(colaFundDesact, exchange);
        Queue colaFundReact = config.foundationReactivatedQueue();
        Binding enlaceFundReact = config.foundationReactivatedBinding(colaFundReact, exchange);
        Binding enlaceFundReactAlt = config.foundationReactivatedAltBinding(colaFundReact, exchange);

        assertThat(enlaceUsuarioAlt.getRoutingKey()).isEqualTo("user.status.changed");
        assertThat(enlaceUsuarioAlt.getDestination()).isEqualTo("styleradar.notificaciones.usuario-estado.q");
        assertThat(enlaceFundDesactAlt.getRoutingKey()).isEqualTo("foundation.deactivated");
        assertThat(enlaceFundDesactAlt.getDestination()).isEqualTo("styleradar.notificaciones.fundacion-desactivada.q");

        assertThat(colaFundReact.getName()).isEqualTo("styleradar.notificaciones.fundacion-reactivada.q");
        assertThat(colaFundReact.isDurable()).isTrue();
        assertThat(colaFundReact.getArguments())
                .containsEntry("x-dead-letter-exchange", "styleradar.dlx")
                .containsEntry("x-dead-letter-routing-key", "styleradar.notificaciones.error");
        assertThat(enlaceFundReact.getRoutingKey()).isEqualTo("styleradar.tiendas.fundacion.reactivada");
        assertThat(enlaceFundReactAlt.getRoutingKey()).isEqualTo("foundation.reactivated");
    }

    @Test
    @DisplayName("El convertidor JSON acepta fechas ISO con o sin sufijo Z, arreglo, blanco y rechaza invalidas")
    void jackson2JsonMessageConverterSoportaFechasUtcYLocales() {
        MessageConverter converter = new RabbitMqConfig().jackson2JsonMessageConverter();

        PrendaRetiradaEventPayload conZ = convertir(converter,
                "{\"eventoId\":\"1\",\"itemId\":\"10\",\"fechaEvento\":\"2026-11-21T22:30:00Z\"}");
        PrendaRetiradaEventPayload sinZ = convertir(converter,
                "{\"eventoId\":\"2\",\"itemId\":\"10\",\"fechaEvento\":\"2026-11-21T22:30:00\"}");
        PrendaRetiradaEventPayload blanca = convertir(converter,
                "{\"eventoId\":\"3\",\"itemId\":\"10\",\"fechaEvento\":\"   \"}");
        PrendaRetiradaEventPayload arreglo = convertir(converter,
                "{\"eventoId\":\"4\",\"itemId\":\"10\",\"fechaEvento\":[2026,11,21,22,30]}");

        assertThat(conZ.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 30));
        assertThat(sinZ.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 30));
        assertThat(blanca.fechaEvento()).isNull();
        assertThat(arreglo.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 30));

        assertThatThrownBy(() -> convertir(converter,
                "{\"eventoId\":\"5\",\"itemId\":\"10\",\"fechaEvento\":\"no-es-fecha\"}"))
                .isInstanceOf(MessageConversionException.class);
    }

    private PrendaRetiradaEventPayload convertir(MessageConverter converter, String json) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);
        Object obj = ((org.springframework.amqp.support.converter.Jackson2JsonMessageConverter) converter)
                .fromMessage(message, ParameterizedTypeReference.forType(PrendaRetiradaEventPayload.class));
        return (PrendaRetiradaEventPayload) obj;
    }
}
