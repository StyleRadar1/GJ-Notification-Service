package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prenda de segunda mano vendida; los demas interesados (con conversacion abierta) reciben el aviso. Ni el comprador
 * ni el vendedor reciben el aviso. {@code interesadosIds} lo resuelve quien publica (Donaciones o ms-chat): si llega
 * vacio no se avisa a nadie.
 */
public record PrendaVendidaEventPayload(
        String eventoId,
        String publicacionId,
        String nombrePrenda,
        Long compradorId,
        List<Long> interesadosIds,
        LocalDateTime fechaEvento,
        Long vendedorId
) {
}
