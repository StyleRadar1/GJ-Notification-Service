package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ticket de vida corta para abrir el stream SSE con EventSource")
public record TicketStreamResponse(
        @Schema(description = "Se envia en ?ticket= al abrir el stream; sirve solo para eso", example = "eyJhbGciOiJIUzI1NiJ9...")
        String ticket,
        @Schema(description = "Segundos que dura el ticket; hay que abrir el stream antes", example = "60")
        long expiraEnSegundos
) {
}
