package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.event.AlertaCreadaDomainEvent;

public interface EventPublisherPort {

    void publicarAlertaCreada(AlertaCreadaDomainEvent event);
}
