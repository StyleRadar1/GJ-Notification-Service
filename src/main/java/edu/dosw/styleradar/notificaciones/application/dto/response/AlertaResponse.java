package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta con la informacion de una alerta configurada por el usuario")
public record AlertaResponse(
        Long id,
        Long usuarioId,
        TipoAlerta tipoAlerta,
        Long itemId,
        String tallaObjetivo,
        Long busquedaGuardadaId,
        CanalNotificacion canal,
        EstadoAlerta estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
