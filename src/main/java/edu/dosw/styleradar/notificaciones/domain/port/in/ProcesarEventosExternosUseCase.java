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

    default Notificacion generarNotificacionDirecta(
            Long usuarioId,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String enlace,
            Long itemId
    ) {
        return generarNotificacionDirecta(usuarioId, tipo, titulo, mensaje, enlace, itemId, null);
    }

    /**
     * Genera una notificacion para el usuario. Si se indica el evento que la origino, una entrega repetida
     * del mismo evento no genera una segunda notificacion para ese usuario (devuelve null).
     */
    Notificacion generarNotificacionDirecta(
            Long usuarioId,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String enlace,
            Long itemId,
            String eventoId
    );

    /**
     * Notifica un pago confirmado. El usuario debe existir en la copia local de cuentas; si no existe se
     * lanza RecursoNoEncontradoException y el mensaje debe ir a la cola de errores.
     */
    Notificacion generarNotificacionPagoConfirmado(Long usuarioId, String plan, String eventoId);
}
