package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;
import java.util.List;

/** Fundacion desactivada; los donantes afectados reciben el aviso para no entregarle donaciones. */
public record FundacionDesactivadaEventPayload(
        String eventoId,
        Long fundacionId,
        String nombreFundacion,
        List<Long> donantesAfectadosIds,
        String motivo,
        LocalDateTime fechaEvento
) {
}
