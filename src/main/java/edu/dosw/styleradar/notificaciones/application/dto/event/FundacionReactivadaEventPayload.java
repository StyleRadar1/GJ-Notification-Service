package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

/**
 * Fundacion reactivada; avisa al representante cuando su fundacion vuelve a quedar habilitada para donaciones.
 */
public record FundacionReactivadaEventPayload(
        @JsonAlias("eventId") String eventoId,
        @JsonAlias("foundationId") Long fundacionId,
        @JsonAlias("foundationName") String nombreFundacion,
        @JsonAlias("representativeId") Long representanteId,
        @JsonAlias("reason") String motivo,
        @JsonAlias("reactivatedAt") LocalDateTime fechaEvento
) {
}
