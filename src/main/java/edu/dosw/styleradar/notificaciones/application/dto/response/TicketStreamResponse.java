package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short-lived ticket to open the SSE stream with EventSource")
public record TicketStreamResponse(
        @Schema(description = "Sent in ?ticket= when opening the stream; it is valid only for that", example = "eyJhbGciOiJIUzI1NiJ9...")
        String ticket,
        @Schema(description = "Seconds the ticket lasts; the stream must be opened before it expires", example = "60")
        long expiraEnSegundos
) {
}
