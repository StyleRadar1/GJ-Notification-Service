package edu.dosw.styleradar.notificaciones.application.dto.event;

import java.time.LocalDateTime;

public record MensajeEnviadoEventPayload(
        String eventoId,
        Long mensajeId,
        Long conversacionId,
        Long remitenteId,
        String nombreRemitente,
        Long destinatarioId,
        String resumenContenido,
        LocalDateTime fechaEnvio
) {
}
