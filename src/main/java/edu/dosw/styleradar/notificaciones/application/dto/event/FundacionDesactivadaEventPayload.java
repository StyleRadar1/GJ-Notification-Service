package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fundacion desactivada; los donantes afectados y el representante reciben el aviso correspondiente.
 */
public record FundacionDesactivadaEventPayload(
        @JsonAlias("eventId") String eventoId,
        @JsonAlias("foundationId") Long fundacionId,
        @JsonAlias("foundationName") String nombreFundacion,
        List<Long> donantesAfectadosIds,
        @JsonAlias("reason") String motivo,
        @JsonAlias("deactivatedAt") LocalDateTime fechaEvento,
        @JsonAlias("representativeId") Long representanteId
) {

    @JsonCreator
    public FundacionDesactivadaEventPayload {
    }

    public FundacionDesactivadaEventPayload(
            String eventoId,
            Long fundacionId,
            String nombreFundacion,
            List<Long> donantesAfectadosIds,
            String motivo,
            LocalDateTime fechaEvento
    ) {
        this(eventoId, fundacionId, nombreFundacion, donantesAfectadosIds, motivo, fechaEvento, null);
    }
}
