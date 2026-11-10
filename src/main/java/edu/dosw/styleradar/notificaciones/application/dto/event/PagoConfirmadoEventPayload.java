package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

/** Pago confirmado por ms-pagos (plan Radar+ o cupos de almacen aliado). */
public record PagoConfirmadoEventPayload(
        String eventoId,
        Long pagoId,
        Long usuarioId,
        String plan,
        LocalDateTime fechaConfirmacion
) {
}
