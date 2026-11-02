package edu.dosw.styleradar.notificaciones.application.service;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import java.util.List;

public interface NotificacionApplicationService {

    List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId, Boolean soloNoLeidas);

    long contarNoLeidas(Long usuarioId);

    Notificacion marcarComoLeida(Long notificacionId, Long usuarioId);

    int marcarTodasComoLeidas(Long usuarioId);
}
