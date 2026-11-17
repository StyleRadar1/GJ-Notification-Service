package edu.dosw.styleradar.notificaciones.application.service;

import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import java.util.List;

public interface NotificacionApplicationService {

    List<Notificacion> listarNotificacionesPorUsuario(Long usuarioId, Boolean soloNoLeidas);

    long contarNoLeidas(Long usuarioId);

    Pagina<Notificacion> listarNotificacionesPaginadas(Long usuarioId, Boolean leida, int page, int size);

    List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId);

    Notificacion marcarComoLeida(Long notificacionId, Long usuarioId);

    int marcarTodasComoLeidas(Long usuarioId);
}

