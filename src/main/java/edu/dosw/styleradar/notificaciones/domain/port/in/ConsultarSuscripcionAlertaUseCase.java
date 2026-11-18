package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import java.util.Optional;

public interface ConsultarSuscripcionAlertaUseCase {

    /** La alerta ACTIVA de tipo ITEM_AGOTADO del usuario para esa prenda y talla, si existe. */
    Optional<Alerta> buscarAlertaActivaDePrenda(Long usuarioId, String itemId, String tallaObjetivo);
}
