package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.port.in.EliminarDatosUsuarioUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EliminarDatosUsuarioUseCaseImpl implements EliminarDatosUsuarioUseCase {

    private final EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;
    private final AlertaRepositoryPort alertaRepositoryPort;
    private final PreferenciaAvisoRepositoryPort preferenciaAvisoRepositoryPort;
    private final CorreoPendienteRepositoryPort correoPendienteRepositoryPort;
    private final BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort;
    private final RealTimeNotificationPort realTimeNotificationPort;

    @Override
    public void eliminarDatosDeUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }
        // Primero se marca la cuenta: desde ese momento ninguna alerta ni evento nuevo le genera avisos.
        // Si algo falla mas abajo, el reintento del mensaje termina el borrado.
        estadoCuentaCopiaRepositoryPort.guardar(
                new EstadoCuentaCopia(usuarioId, null, EstadoCuenta.ELIMINADA, LocalDateTime.now()));
        realTimeNotificationPort.cerrarConexionesDeUsuario(usuarioId);

        int notificaciones = notificacionRepositoryPort.eliminarPorUsuario(usuarioId);
        int alertas = alertaRepositoryPort.eliminarPorUsuario(usuarioId);
        int preferencias = preferenciaAvisoRepositoryPort.eliminarPorUsuario(usuarioId);
        int correos = correoPendienteRepositoryPort.eliminarPorUsuario(usuarioId);
        int busquedas = busquedaGuardadaCopiaRepositoryPort.eliminarPorUsuario(usuarioId);

        log.info("Cuenta {} eliminada: {} notificaciones, {} alertas, {} preferencias, {} correos pendientes "
                        + "y {} busquedas borradas",
                usuarioId, notificaciones, alertas, preferencias, correos, busquedas);
    }
}
