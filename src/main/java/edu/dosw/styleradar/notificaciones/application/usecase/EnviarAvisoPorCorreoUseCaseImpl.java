package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.EnviarAvisoPorCorreoUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EnviarAvisoPorCorreoUseCaseImpl implements EnviarAvisoPorCorreoUseCase {

    private final PreferenciaAvisoRepositoryPort preferenciaRepositoryPort;
    private final EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;
    private final CorreoPort correoPort;

    @Override
    public void enviarSiCorresponde(Notificacion notificacion) {
        if (notificacion == null || notificacion.getUsuarioId() == null || notificacion.getTipo() == null) {
            return;
        }
        try {
            boolean quiereCorreo = preferenciaRepositoryPort
                    .buscar(notificacion.getUsuarioId(), notificacion.getTipo())
                    .map(p -> p.quiereCorreo())
                    .orElse(false);
            if (!quiereCorreo) {
                return;
            }
            String email = estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(notificacion.getUsuarioId())
                    .map(EstadoCuentaCopia::getEmail)
                    .orElse(null);
            if (email == null || email.isBlank()) {
                log.warn("El usuario {} pidio correo pero no hay un email guardado para el", notificacion.getUsuarioId());
                return;
            }
            correoPort.enviar(email, "StyleRadar: " + notificacion.getTitulo(), notificacion.getMensaje());
        } catch (RuntimeException ex) {
            // el aviso en plataforma ya se creo; un fallo aqui nunca debe deshacerlo
            log.error("No se pudo preparar el correo de la notificacion {}", notificacion.getId(), ex);
        }
    }
}
