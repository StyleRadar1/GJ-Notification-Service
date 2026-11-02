package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta con la informacion de una notificacion del usuario")
public record NotificacionResponse(
        Long id,
        Long usuarioId,
        Long alertaId,
        Long itemId,
        Integer cicloReposicion,
        TipoNotificacion tipo,
        CanalNotificacion canal,
        String titulo,
        String mensaje,
        String enlace,
        boolean leida,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaLectura
) {
}
