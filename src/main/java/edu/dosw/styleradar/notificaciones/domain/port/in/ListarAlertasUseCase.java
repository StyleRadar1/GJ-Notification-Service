package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import java.util.List;

public interface ListarAlertasUseCase {
    List<Alerta> listarAlertasPorUsuario(Long usuarioId);
}
