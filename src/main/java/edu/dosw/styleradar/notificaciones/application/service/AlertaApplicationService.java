package edu.dosw.styleradar.notificaciones.application.service;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import java.util.List;
import java.util.Optional;

public interface AlertaApplicationService {

    Alerta crearAlerta(Alerta alerta);

    List<Alerta> listarAlertasPorUsuario(Long usuarioId);

    Alerta desactivarAlerta(Long alertaId, Long usuarioId);

    Alerta activarAlerta(Long alertaId, Long usuarioId);

    Optional<Alerta> buscarAlertaActivaDePrenda(Long usuarioId, String itemId, String tallaObjetivo);
}

