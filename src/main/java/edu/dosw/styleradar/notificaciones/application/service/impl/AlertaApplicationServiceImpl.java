package edu.dosw.styleradar.notificaciones.application.service.impl;

import edu.dosw.styleradar.notificaciones.application.service.AlertaApplicationService;
import edu.dosw.styleradar.notificaciones.domain.event.AlertaCreadaDomainEvent;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AlertaApplicationServiceImpl implements AlertaApplicationService {

    private final AlertaRepositoryPort alertaRepositoryPort;
    private final BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort;
    private final EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;
    private final CatalogoClientPort catalogoClientPort;
    private final EventPublisherPort eventPublisherPort;
    private final AlertaValidator alertaValidator;

    @Override
    public Alerta crearAlerta(Alerta alerta) {
        EstadoCuentaCopia estadoCuenta = alerta != null && alerta.getUsuarioId() != null
                ? estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(alerta.getUsuarioId()).orElse(null)
                : null;

        alertaValidator.validarCreacionAlerta(alerta, estadoCuenta);

        if (TipoAlerta.ITEM_AGOTADO.equals(alerta.getTipoAlerta())) {
            boolean yaExiste = alertaRepositoryPort.existeActivaParaItem(
                    alerta.getUsuarioId(), alerta.getItemId(), alerta.getTallaObjetivo());
            alertaValidator.validarNoDuplicada(yaExiste);

            boolean estaAgotado = catalogoClientPort.verificarItemAgotado(
                    alerta.getItemId(), alerta.getTallaObjetivo());
            alertaValidator.validarItemAgotadoEnCatalogo(estaAgotado, alerta.getItemId());
        } else if (TipoAlerta.BUSQUEDA_GUARDADA.equals(alerta.getTipoAlerta())) {
            BusquedaGuardadaCopia busqueda = busquedaGuardadaCopiaRepositoryPort
                    .buscarPorId(alerta.getBusquedaGuardadaId())
                    .orElse(null);
            alertaValidator.validarBusquedaGuardadaActiva(busqueda, alerta.getUsuarioId());

            boolean yaExiste = alertaRepositoryPort.existeActivaParaBusqueda(
                    alerta.getUsuarioId(), alerta.getBusquedaGuardadaId());
            alertaValidator.validarNoDuplicada(yaExiste);
        }

        if (alerta.getCanal() == null) {
            alerta.setCanal(CanalNotificacion.IN_APP);
        }
        alerta.setEstado(EstadoAlerta.ACTIVA);
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setFechaActualizacion(LocalDateTime.now());

        Alerta guardada = alertaRepositoryPort.guardar(alerta);

        eventPublisherPort.publicarAlertaCreada(new AlertaCreadaDomainEvent(
                UUID.randomUUID().toString(),
                guardada.getId(),
                guardada.getUsuarioId(),
                guardada.getTipoAlerta(),
                guardada.getItemId(),
                guardada.getBusquedaGuardadaId(),
                guardada.getCanal(),
                guardada.getFechaCreacion()
        ));

        return guardada;
    }

    @Override
    public List<Alerta> listarAlertasPorUsuario(Long usuarioId) {
        return alertaRepositoryPort.listarPorUsuario(usuarioId);
    }

    @Override
    public Alerta desactivarAlerta(Long alertaId, Long usuarioId) {
        Alerta alerta = buscarAlerta(alertaId);

        alertaValidator.validarPropiedadAlerta(alerta, usuarioId);
        alerta.desactivar();
        return alertaRepositoryPort.guardar(alerta);
    }

    /**
     * Vuelve a activar una alerta desactivada (SR-RF-32). Aplica las mismas reglas que al crearla: la cuenta
     * no puede estar inactiva, no puede haber otra alerta activa igual (409) y la prenda debe seguir agotada
     * o la busqueda guardada seguir activa. Si ya esta activa, no hace nada.
     */
    @Override
    public Alerta activarAlerta(Long alertaId, Long usuarioId) {
        Alerta alerta = buscarAlerta(alertaId);
        alertaValidator.validarPropiedadAlerta(alerta, usuarioId);
        if (alerta.estaActiva()) {
            return alerta;
        }

        EstadoCuentaCopia estadoCuenta = estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(usuarioId).orElse(null);
        alertaValidator.validarCreacionAlerta(alerta, estadoCuenta);

        if (TipoAlerta.ITEM_AGOTADO.equals(alerta.getTipoAlerta())) {
            alertaValidator.validarNoDuplicada(alertaRepositoryPort.existeActivaParaItem(
                    alerta.getUsuarioId(), alerta.getItemId(), alerta.getTallaObjetivo()));
            alertaValidator.validarItemAgotadoEnCatalogo(
                    catalogoClientPort.verificarItemAgotado(alerta.getItemId(), alerta.getTallaObjetivo()),
                    alerta.getItemId());
        } else if (TipoAlerta.BUSQUEDA_GUARDADA.equals(alerta.getTipoAlerta())) {
            BusquedaGuardadaCopia busqueda = busquedaGuardadaCopiaRepositoryPort
                    .buscarPorId(alerta.getBusquedaGuardadaId())
                    .orElse(null);
            alertaValidator.validarBusquedaGuardadaActiva(busqueda, alerta.getUsuarioId());
            alertaValidator.validarNoDuplicada(alertaRepositoryPort.existeActivaParaBusqueda(
                    alerta.getUsuarioId(), alerta.getBusquedaGuardadaId()));
        }

        alerta.activar();
        return alertaRepositoryPort.guardar(alerta);
    }

    /** Una talla en blanco equivale a «sin talla»; la comparacion ignora mayusculas y espacios. */
    @Override
    public Optional<Alerta> buscarAlertaActivaDePrenda(Long usuarioId, String itemId, String tallaObjetivo) {
        String talla = normalizar(tallaObjetivo);
        return alertaRepositoryPort.listarPorUsuario(usuarioId).stream()
                .filter(Alerta::estaActiva)
                .filter(a -> TipoAlerta.ITEM_AGOTADO.equals(a.getTipoAlerta()))
                .filter(a -> itemId != null && itemId.trim().equals(a.getItemId()))
                .filter(a -> java.util.Objects.equals(talla, normalizar(a.getTallaObjetivo())))
                .findFirst();
    }

    private static String normalizar(String talla) {
        return talla == null || talla.isBlank() ? null : talla.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private Alerta buscarAlerta(Long alertaId) {
        return alertaRepositoryPort.buscarPorId(alertaId).orElseThrow(() -> {
            log.debug("No existe la alerta {}", alertaId);
            return new RecursoNoEncontradoException("No se encontro la alerta");
        });
    }
}

