package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.EnviarAvisoPorCorreoUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import edu.dosw.styleradar.notificaciones.domain.validator.EvaluadorCriteriosMatcher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcesarEventosExternosUseCaseImpl implements ProcesarEventosExternosUseCase {

    private final AlertaRepositoryPort alertaRepositoryPort;
    private final NotificacionRepositoryPort notificacionRepositoryPort;
    private final BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort;
    private final EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;
    private final RealTimeNotificationPort realTimeNotificationPort;
    private final EvaluadorCriteriosMatcher evaluadorCriteriosMatcher;
    private final EnviarAvisoPorCorreoUseCase enviarAvisoPorCorreoUseCase;

    @Override
    public List<Notificacion> procesarDisponibilidadPrenda(DatosPrendaVO datosPrenda) {
        List<Notificacion> generadas = new ArrayList<>();
        if (datosPrenda == null || datosPrenda.stockDisponible() == null || datosPrenda.stockDisponible() <= 0) {
            return generadas;
        }

        Integer ciclo = datosPrenda.cicloReposicion() != null ? datosPrenda.cicloReposicion() : 1;

        List<Alerta> alertasItem = alertaRepositoryPort.listarActivasPorItem(datosPrenda.itemId());
        for (Alerta alerta : alertasItem) {
            if (evaluadorCriteriosMatcher.coincideItemAgotado(alerta.getItemId(), alerta.getTallaObjetivo(), datosPrenda)) {
                generarSiElegible(
                        alerta,
                        datosPrenda,
                        ciclo,
                        TipoNotificacion.REPOSICION_STOCK,
                        "Prenda disponible nuevamente",
                        "La prenda '" + datosPrenda.nombrePrenda() + "' volvio a tener unidades disponibles.",
                        generadas
                );
            }
        }

        List<Alerta> alertasBusqueda = alertaRepositoryPort.listarActivasPorTipo(TipoAlerta.BUSQUEDA_GUARDADA);
        for (Alerta alerta : alertasBusqueda) {
            Optional<BusquedaGuardadaCopia> copiaOpt = busquedaGuardadaCopiaRepositoryPort
                    .buscarPorId(alerta.getBusquedaGuardadaId());
            if (copiaOpt.isPresent() && copiaOpt.get().isActiva()) {
                BusquedaGuardadaCopia copia = copiaOpt.get();
                if (evaluadorCriteriosMatcher.coincideConBusqueda(copia.getCriterios(), datosPrenda)) {
                    generarSiElegible(
                            alerta,
                            datosPrenda,
                            ciclo,
                            TipoNotificacion.COINCIDENCIA_BUSQUEDA,
                            "Nueva coincidencia con tu busqueda: " + copia.getNombre(),
                            "La prenda '" + datosPrenda.nombrePrenda() + "' coincide con los criterios de tu busqueda guardada.",
                            generadas
                    );
                }
            }
        }

        return generadas;
    }

    private void generarSiElegible(
            Alerta alerta,
            DatosPrendaVO datosPrenda,
            Integer ciclo,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            List<Notificacion> generadas
    ) {
        if (!usuarioActivo(alerta.getUsuarioId())) {
            return;
        }
        if (notificacionRepositoryPort.existePorAlertaItemYCiclo(alerta.getId(), datosPrenda.itemId(), ciclo)) {
            return;
        }

        Notificacion nueva = new Notificacion(
                null,
                alerta.getUsuarioId(),
                alerta.getId(),
                datosPrenda.itemId(),
                ciclo,
                tipo,
                alerta.getCanal(),
                titulo,
                mensaje,
                "/catalogo/prendas/" + datosPrenda.itemId(),
                false,
                LocalDateTime.now(),
                null
        );

        Notificacion guardada = notificacionRepositoryPort.guardar(nueva);
        if (guardada.getId() == null) {
            // El puerto devuelve la notificacion sin id cuando otra entrega ya registro este ciclo
            return;
        }
        realTimeNotificationPort.emitirNotificacionTiempoReal(guardada.getUsuarioId(), guardada);
        enviarAvisoPorCorreoUseCase.enviarSiCorresponde(guardada);
        generadas.add(guardada);
    }

    @Override
    public void desactivarAlertasPorItemEliminado(Long itemId) {
        if (itemId != null) {
            alertaRepositoryPort.desactivarPorItem(itemId);
        }
    }

    @Override
    public void sincronizarBusquedaGuardada(BusquedaGuardadaCopia busqueda, boolean eliminada) {
        if (busqueda == null || busqueda.getBusquedaId() == null) {
            return;
        }
        if (eliminada || !busqueda.isActiva()) {
            busqueda.setActiva(false);
            busquedaGuardadaCopiaRepositoryPort.guardar(busqueda);
            alertaRepositoryPort.desactivarPorBusquedaGuardada(busqueda.getBusquedaId());
        } else {
            busqueda.setFechaSincronizacion(LocalDateTime.now());
            busquedaGuardadaCopiaRepositoryPort.guardar(busqueda);
        }
    }

    @Override
    public void actualizarEstadoCuenta(Long usuarioId, String email, EstadoCuenta nuevoEstado) {
        if (usuarioId == null || nuevoEstado == null) {
            return;
        }
        EstadoCuentaCopia copia = new EstadoCuentaCopia(usuarioId, email, nuevoEstado, LocalDateTime.now());
        estadoCuentaCopiaRepositoryPort.guardar(copia);
    }

    @Override
    public Notificacion generarNotificacionPagoConfirmado(Long usuarioId, String plan, String eventoId) {
        estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El usuario " + usuarioId + " no existe en la copia local de cuentas"));
        String detalle = plan != null && !plan.isBlank()
                ? "Tu pago se confirmo y tu plan " + plan + " ya esta activo."
                : "Tu pago se confirmo y ya esta activo.";
        return generarNotificacionDirecta(
                usuarioId, TipoNotificacion.PAGO_CONFIRMADO, "Pago confirmado", detalle, "/planes/mi-plan", null, eventoId);
    }

    @Override
    public Notificacion generarNotificacionDirecta(
            Long usuarioId,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String enlace,
            Long itemId,
            String eventoId
    ) {
        if (usuarioId == null || !usuarioActivo(usuarioId)) {
            return null;
        }
        if (eventoId != null && notificacionRepositoryPort.existePorEventoYUsuario(eventoId, usuarioId)) {
            return null;
        }
        Notificacion notificacion = new Notificacion(
                null,
                usuarioId,
                null,
                itemId,
                null,
                tipo,
                CanalNotificacion.IN_APP,
                titulo,
                mensaje,
                enlace,
                false,
                LocalDateTime.now(),
                null
        );
        notificacion.setEventoId(eventoId);
        Notificacion guardada = notificacionRepositoryPort.guardar(notificacion);
        if (guardada.getId() == null) {
            return null;
        }
        realTimeNotificationPort.emitirNotificacionTiempoReal(usuarioId, guardada);
        enviarAvisoPorCorreoUseCase.enviarSiCorresponde(guardada);
        return guardada;
    }

    private boolean usuarioActivo(Long usuarioId) {
        return estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(usuarioId)
                .map(EstadoCuentaCopia::puedeRecibirNotificaciones)
                .orElse(true);
    }
}
