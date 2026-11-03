package edu.dosw.styleradar.notificaciones.domain.event;

import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import java.time.LocalDateTime;

public record AlertaCreadaDomainEvent(
        String eventoId,
        Long alertaId,
        Long usuarioId,
        TipoAlerta tipoAlerta,
        Long itemId,
        Long busquedaGuardadaId,
        CanalNotificacion canal,
        LocalDateTime fechaCreacion
) {
}
