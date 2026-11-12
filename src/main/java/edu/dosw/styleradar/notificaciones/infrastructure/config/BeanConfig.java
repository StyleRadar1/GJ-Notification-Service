package edu.dosw.styleradar.notificaciones.infrastructure.config;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.NotificacionApplicationService;
import edu.dosw.styleradar.notificaciones.application.service.impl.AlertaApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.application.service.impl.NotificacionApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.CrearAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.DesactivarAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarAlertasUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarNotificacionesUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.EnviarAvisoPorCorreoUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.MarcarNotificacionLeidaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.PreferenciasAvisosUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ProcesarEventosExternosUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.port.in.CrearAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.DesactivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.EnviarAvisoPorCorreoUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarAlertasUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ListarNotificacionesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.MarcarNotificacionLeidaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.PreferenciasAvisosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
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
    public AlertaApplicationService alertaApplicationService(
            AlertaRepositoryPort alertaRepositoryPort,
            BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            CatalogoClientPort catalogoClientPort,
            EventPublisherPort eventPublisherPort,
            AlertaValidator alertaValidator
    ) {
        return new AlertaApplicationServiceImpl(
                alertaRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                catalogoClientPort,
                eventPublisherPort,
                alertaValidator
        );
    }

    @Bean
    public NotificacionApplicationService notificacionApplicationService(
            NotificacionRepositoryPort notificacionRepositoryPort
    ) {
        return new NotificacionApplicationServiceImpl(notificacionRepositoryPort);
    }

    @Bean
    public CrearAlertaUseCase crearAlertaUseCase(AlertaApplicationService alertaApplicationService) {
        return new CrearAlertaUseCaseImpl(alertaApplicationService);
    }

    @Bean
    public ListarAlertasUseCase listarAlertasUseCase(AlertaApplicationService alertaApplicationService) {
        return new ListarAlertasUseCaseImpl(alertaApplicationService);
    }

    @Bean
    public DesactivarAlertaUseCase desactivarAlertaUseCase(AlertaApplicationService alertaApplicationService) {
        return new DesactivarAlertaUseCaseImpl(alertaApplicationService);
    }

    @Bean
    public ListarNotificacionesUseCase listarNotificacionesUseCase(
            NotificacionApplicationService notificacionApplicationService
    ) {
        return new ListarNotificacionesUseCaseImpl(notificacionApplicationService);
    }

    @Bean
    public MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase(
            NotificacionApplicationService notificacionApplicationService
    ) {
        return new MarcarNotificacionLeidaUseCaseImpl(notificacionApplicationService);
    }

    @Bean
    public ProcesarEventosExternosUseCase procesarEventosExternosUseCase(
            AlertaRepositoryPort alertaRepositoryPort,
            NotificacionRepositoryPort notificacionRepositoryPort,
            BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            RealTimeNotificationPort realTimeNotificationPort,
            EvaluadorCriteriosMatcher evaluadorCriteriosMatcher,
            EnviarAvisoPorCorreoUseCase enviarAvisoPorCorreoUseCase
    ) {
        return new ProcesarEventosExternosUseCaseImpl(
                alertaRepositoryPort,
                notificacionRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                realTimeNotificationPort,
                evaluadorCriteriosMatcher,
                enviarAvisoPorCorreoUseCase
        );
    }

    @Bean
    public PreferenciasAvisosUseCase preferenciasAvisosUseCase(PreferenciaAvisoRepositoryPort preferenciaAvisoRepositoryPort) {
        return new PreferenciasAvisosUseCaseImpl(preferenciaAvisoRepositoryPort);
    }

    @Bean
    public EnviarAvisoPorCorreoUseCase enviarAvisoPorCorreoUseCase(
            PreferenciaAvisoRepositoryPort preferenciaAvisoRepositoryPort,
            EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort,
            CorreoPort correoPort
    ) {
        return new EnviarAvisoPorCorreoUseCaseImpl(preferenciaAvisoRepositoryPort, estadoCuentaCopiaRepositoryPort, correoPort);
    }
}
