package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;
import java.util.List;

/** Prenda de segunda mano vendida; los demas interesados (con conversacion abierta) reciben el aviso. */
public record PrendaVendidaEventPayload(
        String eventoId,
        Long publicacionId,
        String nombrePrenda,
        Long compradorId,
        List<Long> interesadosIds,
        LocalDateTime fechaEvento
) {
}
