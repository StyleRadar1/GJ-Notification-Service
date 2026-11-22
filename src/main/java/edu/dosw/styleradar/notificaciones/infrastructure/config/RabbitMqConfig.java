package edu.dosw.styleradar.notificaciones.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.LocalDateTime;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${application.messaging.exchange:styleradar.exchange}")
    private String exchangeName;

    @Value("${application.messaging.dlx:styleradar.dlx}")
    private String dlxName;

    @Value("${application.messaging.queues.inventory-updated:styleradar.notificaciones.inventario-actualizado.q}")
    private String inventoryUpdatedQueueName;

    @Value("${application.messaging.queues.garment-published:styleradar.notificaciones.prenda-publicada.q}")
    private String garmentPublishedQueueName;

    @Value("${application.messaging.queues.user-status-changed:styleradar.notificaciones.usuario-estado.q}")
    private String userStatusChangedQueueName;

    @Value("${application.messaging.queues.saved-search-synced:styleradar.notificaciones.busqueda-guardada.q}")
    private String savedSearchSyncedQueueName;

    @Value("${application.messaging.queues.message-sent:styleradar.notificaciones.mensaje-enviado.q}")
    private String messageSentQueueName;

    @Value("${application.messaging.queues.donation-changed:styleradar.notificaciones.donacion-cambiada.q}")
    private String donationChangedQueueName;

    @Value("${application.messaging.queues.moderation-changed:styleradar.notificaciones.moderacion-cambiada.q}")
    private String moderationChangedQueueName;

    @Value("${application.messaging.queues.garment-deleted:styleradar.notificaciones.prenda-retirada.q}")
    private String garmentDeletedQueueName;

    @Value("${application.messaging.queues.donation-requested:styleradar.notificaciones.solicitud-donacion.q}")
    private String donationRequestedQueueName;

    @Value("${application.messaging.queues.garment-sold:styleradar.notificaciones.prenda-vendida.q}")
    private String garmentSoldQueueName;

    @Value("${application.messaging.queues.payment-confirmed:styleradar.notificaciones.pago-confirmado.q}")
    private String paymentConfirmedQueueName;

    @Value("${application.messaging.queues.foundation-deactivated:styleradar.notificaciones.fundacion-desactivada.q}")
    private String foundationDeactivatedQueueName;

    @Value("${application.messaging.queues.foundation-reactivated:styleradar.notificaciones.fundacion-reactivada.q}")
    private String foundationReactivatedQueueName;

    @Value("${application.messaging.queues.dlq:styleradar.notificaciones.dlq}")
    private String dlqName;

    @Value("${application.messaging.routing-keys.inventory-updated:styleradar.inventario.actualizado}")
    private String inventoryUpdatedKey;

    @Value("${application.messaging.routing-keys.garment-published:styleradar.catalogo.prenda.publicada}")
    private String garmentPublishedKey;

    @Value("${application.messaging.routing-keys.user-status-changed:styleradar.usuarios.estado.cambiado}")
    private String userStatusChangedKey;

    /** Clave unificada v2 (CADCHE ms-usuarios / ms-auth); la misma cola recibe ambas. */
    @Value("${application.messaging.routing-keys.user-status-changed-alt:user.status.changed}")
    private String userStatusChangedAltKey;

    @Value("${application.messaging.routing-keys.saved-search-synced:styleradar.usuarios.busqueda.sincronizada}")
    private String savedSearchSyncedKey;

    @Value("${application.messaging.routing-keys.message-sent:styleradar.chat.mensaje.enviado}")
    private String messageSentKey;

    @Value("${application.messaging.routing-keys.donation-changed:styleradar.donaciones.estado.cambiado}")
    private String donationChangedKey;

    @Value("${application.messaging.routing-keys.moderation-changed:styleradar.catalogo.moderacion.cambiada}")
    private String moderationChangedKey;

    @Value("${application.messaging.routing-keys.garment-deleted:styleradar.catalogo.prenda.retirada}")
    private String garmentDeletedKey;

    @Value("${application.messaging.routing-keys.donation-requested:styleradar.donaciones.solicitud.creada}")
    private String donationRequestedKey;

    @Value("${application.messaging.routing-keys.garment-sold:styleradar.segundavida.prenda.vendida}")
    private String garmentSoldKey;

    /** Clave con la que Donaciones publica la venta; la misma cola recibe las dos. */
    @Value("${application.messaging.routing-keys.garment-sold-alt:styleradar.segunda-mano.prenda.vendida}")
    private String garmentSoldAltKey;

    @Value("${application.messaging.routing-keys.payment-confirmed:styleradar.pagos.pago.confirmado}")
    private String paymentConfirmedKey;

    @Value("${application.messaging.routing-keys.foundation-deactivated:styleradar.tiendas.fundacion.desactivada}")
    private String foundationDeactivatedKey;

    /** Clave v2 publicada por ms-tiendas (CADCHE); la misma cola recibe ambas. */
    @Value("${application.messaging.routing-keys.foundation-deactivated-alt:foundation.deactivated}")
    private String foundationDeactivatedAltKey;

    @Value("${application.messaging.routing-keys.foundation-reactivated:styleradar.tiendas.fundacion.reactivada}")
    private String foundationReactivatedKey;

    /** Clave v2 publicada por ms-tiendas (CADCHE) al reactivar una fundacion aliada. */
    @Value("${application.messaging.routing-keys.foundation-reactivated-alt:foundation.reactivated}")
    private String foundationReactivatedAltKey;

    @Value("${application.messaging.routing-keys.dlq:styleradar.notificaciones.error}")
    private String dlqRoutingKey;

    @Bean
    public TopicExchange styleradarExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    @Bean
    public Queue inventoryUpdatedQueue() {
        return buildDurableQueueWithDlq(inventoryUpdatedQueueName);
    }

    @Bean
    public Queue garmentPublishedQueue() {
        return buildDurableQueueWithDlq(garmentPublishedQueueName);
    }

    @Bean
    public Queue userStatusChangedQueue() {
        return buildDurableQueueWithDlq(userStatusChangedQueueName);
    }

    @Bean
    public Queue savedSearchSyncedQueue() {
        return buildDurableQueueWithDlq(savedSearchSyncedQueueName);
    }

    @Bean
    public Queue messageSentQueue() {
        return buildDurableQueueWithDlq(messageSentQueueName);
    }

    @Bean
    public Queue donationChangedQueue() {
        return buildDurableQueueWithDlq(donationChangedQueueName);
    }

    @Bean
    public Queue moderationChangedQueue() {
        return buildDurableQueueWithDlq(moderationChangedQueueName);
    }

    @Bean
    public Queue garmentDeletedQueue() {
        return buildDurableQueueWithDlq(garmentDeletedQueueName);
    }

    @Bean
    public Binding inventoryUpdatedBinding(Queue inventoryUpdatedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(inventoryUpdatedQueue).to(styleradarExchange).with(inventoryUpdatedKey);
    }

    @Bean
    public Binding garmentPublishedBinding(Queue garmentPublishedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(garmentPublishedQueue).to(styleradarExchange).with(garmentPublishedKey);
    }

    @Bean
    public Binding userStatusChangedBinding(Queue userStatusChangedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(userStatusChangedQueue).to(styleradarExchange).with(userStatusChangedKey);
    }

    @Bean
    public Binding userStatusChangedAltBinding(Queue userStatusChangedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(userStatusChangedQueue).to(styleradarExchange).with(userStatusChangedAltKey);
    }

    @Bean
    public Binding savedSearchSyncedBinding(Queue savedSearchSyncedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(savedSearchSyncedQueue).to(styleradarExchange).with(savedSearchSyncedKey);
    }

    @Bean
    public Binding messageSentBinding(Queue messageSentQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(messageSentQueue).to(styleradarExchange).with(messageSentKey);
    }

    @Bean
    public Binding donationChangedBinding(Queue donationChangedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(donationChangedQueue).to(styleradarExchange).with(donationChangedKey);
    }

    @Bean
    public Binding moderationChangedBinding(Queue moderationChangedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(moderationChangedQueue).to(styleradarExchange).with(moderationChangedKey);
    }

    @Bean
    public Binding garmentDeletedBinding(Queue garmentDeletedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(garmentDeletedQueue).to(styleradarExchange).with(garmentDeletedKey);
    }

    @Bean
    public Queue donationRequestedQueue() {
        return buildDurableQueueWithDlq(donationRequestedQueueName);
    }

    @Bean
    public Queue garmentSoldQueue() {
        return buildDurableQueueWithDlq(garmentSoldQueueName);
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return buildDurableQueueWithDlq(paymentConfirmedQueueName);
    }

    @Bean
    public Queue foundationDeactivatedQueue() {
        return buildDurableQueueWithDlq(foundationDeactivatedQueueName);
    }

    @Bean
    public Binding foundationDeactivatedBinding(Queue foundationDeactivatedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(foundationDeactivatedQueue).to(styleradarExchange).with(foundationDeactivatedKey);
    }

    @Bean
    public Binding foundationDeactivatedAltBinding(Queue foundationDeactivatedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(foundationDeactivatedQueue).to(styleradarExchange).with(foundationDeactivatedAltKey);
    }

    @Bean
    public Queue foundationReactivatedQueue() {
        return buildDurableQueueWithDlq(foundationReactivatedQueueName);
    }

    @Bean
    public Binding foundationReactivatedBinding(Queue foundationReactivatedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(foundationReactivatedQueue).to(styleradarExchange).with(foundationReactivatedKey);
    }

    @Bean
    public Binding foundationReactivatedAltBinding(Queue foundationReactivatedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(foundationReactivatedQueue).to(styleradarExchange).with(foundationReactivatedAltKey);
    }

    @Bean
    public Binding donationRequestedBinding(Queue donationRequestedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(donationRequestedQueue).to(styleradarExchange).with(donationRequestedKey);
    }

    @Bean
    public Binding garmentSoldBinding(Queue garmentSoldQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(garmentSoldQueue).to(styleradarExchange).with(garmentSoldKey);
    }

    @Bean
    public Binding garmentSoldAltBinding(Queue garmentSoldQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(garmentSoldQueue).to(styleradarExchange).with(garmentSoldAltKey);
    }

    @Bean
    public Binding paymentConfirmedBinding(Queue paymentConfirmedQueue, TopicExchange styleradarExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue).to(styleradarExchange).with(paymentConfirmedKey);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        SimpleModule moduloFechas = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .addModule(moduloFechas)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private Queue buildDurableQueueWithDlq(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }
}
