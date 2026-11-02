package edu.dosw.styleradar.notificaciones.domain.validator;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ConflictoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;

public class AlertaValidator {

    public void validarCreacionAlerta(Alerta alerta, EstadoCuentaCopia estadoCuenta) {
        if (alerta == null) {
            throw new ReglaDeNegocioException("Los datos de la alerta son obligatorios");
        }
        if (alerta.getUsuarioId() == null || alerta.getUsuarioId() <= 0) {
            throw new ReglaDeNegocioException("El identificador del usuario es obligatorio y debe ser positivo");
        }
        if (estadoCuenta != null && !estadoCuenta.puedeRecibirNotificaciones()) {
            throw new ReglaDeNegocioException(
                    "La cuenta del usuario se encuentra en estado " + estadoCuenta.getEstado()
                            + " y no puede registrar alertas");
        }
        if (alerta.getTipoAlerta() == null) {
            throw new ReglaDeNegocioException("El tipo de alerta es obligatorio");
        }

        if (TipoAlerta.ITEM_AGOTADO.equals(alerta.getTipoAlerta())) {
            if (alerta.getItemId() == null || alerta.getItemId() <= 0) {
                throw new ReglaDeNegocioException("Para alertas de item agotado se debe especificar un itemId valido");
            }
            if (alerta.getBusquedaGuardadaId() != null) {
                throw new ReglaDeNegocioException("Una alerta de item agotado no puede asociarse simultaneamente a una busqueda guardada");
            }
        } else if (TipoAlerta.BUSQUEDA_GUARDADA.equals(alerta.getTipoAlerta())) {
            if (alerta.getBusquedaGuardadaId() == null || alerta.getBusquedaGuardadaId() <= 0) {
                throw new ReglaDeNegocioException("Para alertas de busqueda guardada se debe especificar un busquedaGuardadaId valido");
            }
            if (alerta.getItemId() != null) {
                throw new ReglaDeNegocioException("Una alerta de busqueda guardada no puede asociarse simultaneamente a un item especifico");
            }
        }
    }

    public void validarItemAgotadoEnCatalogo(boolean estaAgotado, Long itemId) {
        if (!estaAgotado) {
            throw new ReglaDeNegocioException(
                    "No se puede crear una alerta de reposicion porque el item " + itemId + " actualmente tiene stock disponible");
        }
    }

    public void validarBusquedaGuardadaActiva(BusquedaGuardadaCopia busqueda, Long usuarioId) {
        if (busqueda == null || !busqueda.isActiva()) {
            throw new ReglaDeNegocioException("La busqueda guardada indicada no existe o no se encuentra activa");
        }
        if (!busqueda.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tiene permisos para crear alertas sobre una busqueda guardada de otro usuario");
        }
    }

    public void validarNoDuplicada(boolean yaExisteActiva) {
        if (yaExisteActiva) {
            throw new ConflictoException("Ya existe una alerta activa configurada para este recurso");
        }
    }

    public void validarPropiedadAlerta(Alerta alerta, Long usuarioAutenticadoId) {
        if (!alerta.getUsuarioId().equals(usuarioAutenticadoId)) {
            throw new AccesoDenegadoException("No tiene permisos para modificar o desactivar una alerta de otro usuario");
        }
    }
}
