package edu.dosw.styleradar.notificaciones.infrastructure.config;

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

    @Value("${application.messaging.queues.dlq:styleradar.notificaciones.dlq}")
    private String dlqName;

    @Value("${application.messaging.routing-keys.inventory-updated:styleradar.inventario.actualizado}")
    private String inventoryUpdatedKey;

    @Value("${application.messaging.routing-keys.garment-published:styleradar.catalogo.prenda.publicada}")
    private String garmentPublishedKey;

    @Value("${application.messaging.routing-keys.user-status-changed:styleradar.usuarios.estado.cambiado}")
    private String userStatusChangedKey;

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
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue buildDurableQueueWithDlq(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }
}
