package edu.dosw.styleradar.notificaciones.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con el conteo de notificaciones no leidas del usuario")
public record ContadorNoLeidasResponse(
        long usuarioId,
        long cantidadNoLeidas
) {
}
