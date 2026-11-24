package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp;

import edu.dosw.styleradar.notificaciones.application.dto.event.CuentaEliminadaEventPayload;
import edu.dosw.styleradar.notificaciones.domain.port.in.EliminarDatosUsuarioUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Atiende la eliminacion de una cuenta en ms-Users para borrar sus datos de notificaciones. */
@Component
@RequiredArgsConstructor
public class CuentaEliminadaEventsListener {

    private final EliminarDatosUsuarioUseCase eliminarDatosUsuarioUseCase;

    @RabbitListener(queues = "${application.messaging.queues.account-deleted:styleradar.notificaciones.cuenta-eliminada.q}")
    public void onCuentaEliminada(CuentaEliminadaEventPayload payload) {
        if (payload == null || payload.usuarioId() == null) {
            throw new AmqpRejectAndDontRequeueException("Evento CuentaEliminada invalido: sin usuarioId");
        }
        eliminarDatosUsuarioUseCase.eliminarDatosDeUsuario(payload.usuarioId());
    }
}
