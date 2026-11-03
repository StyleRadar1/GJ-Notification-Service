package edu.dosw.styleradar.notificaciones.domain.port.in;

import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import java.util.List;

public interface ProcesarEventosExternosUseCase {

    List<Notificacion> procesarDisponibilidadPrenda(DatosPrendaVO datosPrenda);

    void desactivarAlertasPorItemEliminado(Long itemId);

    void sincronizarBusquedaGuardada(BusquedaGuardadaCopia busqueda, boolean eliminada);

    void actualizarEstadoCuenta(Long usuarioId, String email, EstadoCuenta nuevoEstado);

    Notificacion generarNotificacionDirecta(
            Long usuarioId,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String enlace,
            Long itemId
    );
}
