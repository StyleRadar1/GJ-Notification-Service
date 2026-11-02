package edu.dosw.styleradar.notificaciones.application.service;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import java.util.List;

public interface AlertaApplicationService {

    Alerta crearAlerta(Alerta alerta);

    List<Alerta> listarAlertasPorUsuario(Long usuarioId);

    Alerta desactivarAlerta(Long alertaId, Long usuarioId);
}
