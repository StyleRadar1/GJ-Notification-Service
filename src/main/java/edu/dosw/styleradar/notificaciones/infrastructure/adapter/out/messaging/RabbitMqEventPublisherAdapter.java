package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.messaging;

import edu.dosw.styleradar.notificaciones.domain.event.AlertaCreadaDomainEvent;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqEventPublisherAdapter implements EventPublisherPort {

    @Nullable
    private final RabbitTemplate rabbitTemplate;

    @Value("${application.messaging.exchange:styleradar.exchange}")
    private String exchange;

    @Value("${application.messaging.routing-keys.alert-created:styleradar.notificaciones.alerta.creada}")
    private String alertCreatedRoutingKey;

    @Override
    public void publicarAlertaCreada(AlertaCreadaDomainEvent event) {
        if (rabbitTemplate == null || event == null) {
            log.debug("RabbitTemplate no disponible o evento nulo; omitiendo publicacion de AlertaCreada");
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchange, alertCreatedRoutingKey, event);
        } catch (Exception ex) {
            log.warn("Error al publicar evento AlertaCreada en RabbitMQ para alerta {}: {}",
                    event.alertaId(), ex.getMessage());
        }
    }
}
