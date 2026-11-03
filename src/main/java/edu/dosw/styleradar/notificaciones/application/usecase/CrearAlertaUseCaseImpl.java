package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.AlertaApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.CrearAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CrearAlertaUseCaseImpl implements CrearAlertaUseCase {

    private final AlertaApplicationService alertaApplicationService;

    public CrearAlertaUseCaseImpl(
            AlertaRepositoryPort alertaRepositoryPort,
            BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            CatalogoClientPort catalogoClientPort,
            EventPublisherPort eventPublisherPort,
            AlertaValidator alertaValidator
    ) {
        this(new AlertaApplicationServiceImpl(
                alertaRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                catalogoClientPort,
                eventPublisherPort,
                alertaValidator
        ));
    }

    @Override
    public Alerta crearAlerta(Alerta alerta) {
        return alertaApplicationService.crearAlerta(alerta);
    }
}
