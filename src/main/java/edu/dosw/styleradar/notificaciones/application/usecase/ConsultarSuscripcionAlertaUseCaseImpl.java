package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ConsultarSuscripcionAlertaUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarSuscripcionAlertaUseCaseImpl implements ConsultarSuscripcionAlertaUseCase {

    private final AlertaApplicationService alertaApplicationService;

    @Override
    public Optional<Alerta> buscarAlertaActivaDePrenda(Long usuarioId, String itemId, String tallaObjetivo) {
        return alertaApplicationService.buscarAlertaActivaDePrenda(usuarioId, itemId, tallaObjetivo);
    }
}
