package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DesactivarAlertaUseCaseImpl implements DesactivarAlertaUseCase {

    private final AlertaRepositoryPort alertaRepositoryPort;
    private final AlertaValidator alertaValidator;

    @Override
    public Alerta desactivarAlerta(Long alertaId, Long usuarioId) {
        Alerta alerta = alertaRepositoryPort.buscarPorId(alertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la alerta con ID: " + alertaId));

        alertaValidator.validarPropiedadAlerta(alerta, usuarioId);
        alerta.desactivar();
        return alertaRepositoryPort.guardar(alerta);
    }
}
