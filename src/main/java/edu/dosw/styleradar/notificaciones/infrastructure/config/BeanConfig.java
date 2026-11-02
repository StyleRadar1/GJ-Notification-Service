package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.application.usecase.CrearAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.DesactivarAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarAlertasUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarNotificacionesUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.MarcarNotificacionLeidaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ProcesarEventosExternosUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.port.in.CrearAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import edu.dosw.styleradar.notificaciones.domain.validator.EvaluadorCriteriosMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public AlertaValidator alertaValidator() {
        return new AlertaValidator();
    }

    @Bean
    public EvaluadorCriteriosMatcher evaluadorCriteriosMatcher() {
        return new EvaluadorCriteriosMatcher();
    }

    @Bean
    public CrearAlertaUseCase crearAlertaUseCase(
            AlertaRepositoryPort alertaRepositoryPort,
            BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            CatalogoClientPort catalogoClientPort,
            EventPublisherPort eventPublisherPort,
            AlertaValidator alertaValidator
    ) {
        return new CrearAlertaUseCaseImpl(
                alertaRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                catalogoClientPort,
                eventPublisherPort,
                alertaValidator
        );
    }

    @Bean
    public ListarAlertasUseCase listarAlertasUseCase(AlertaRepositoryPort alertaRepositoryPort) {
        return new ListarAlertasUseCaseImpl(alertaRepositoryPort);
    }

    @Bean
    public DesactivarAlertaUseCase desactivarAlertaUseCase(
            AlertaRepositoryPort alertaRepositoryPort,
            AlertaValidator alertaValidator
    ) {
        return new DesactivarAlertaUseCaseImpl(alertaRepositoryPort, alertaValidator);
    }

    @Bean
    public ListarNotificacionesUseCase listarNotificacionesUseCase(
            NotificacionRepositoryPort notificacionRepositoryPort
    ) {
        return new ListarNotificacionesUseCaseImpl(notificacionRepositoryPort);
    }

    @Bean
    public MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase(
            NotificacionRepositoryPort notificacionRepositoryPort
    ) {
        return new MarcarNotificacionLeidaUseCaseImpl(notificacionRepositoryPort);
    }

    @Bean
    public ProcesarEventosExternosUseCase procesarEventosExternosUseCase(
            AlertaRepositoryPort alertaRepositoryPort,
            NotificacionRepositoryPort notificacionRepositoryPort,
            BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            RealTimeNotificationPort realTimeNotificationPort,
            EvaluadorCriteriosMatcher evaluadorCriteriosMatcher
    ) {
        return new ProcesarEventosExternosUseCaseImpl(
                alertaRepositoryPort,
                notificacionRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                realTimeNotificationPort,
                evaluadorCriteriosMatcher
        );
    }
}
