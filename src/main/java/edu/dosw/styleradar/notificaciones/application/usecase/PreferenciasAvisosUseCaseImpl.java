package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.PreferenciasAvisosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PreferenciasAvisosUseCaseImpl implements PreferenciasAvisosUseCase {

    private final PreferenciaAvisoRepositoryPort preferenciaRepositoryPort;

    @Override
    public List<PreferenciaAviso> obtenerPreferencias(Long usuarioId) {
        Map<TipoNotificacion, PreferenciaAviso> guardadas = preferenciaRepositoryPort.listarPorUsuario(usuarioId).stream()
                .collect(Collectors.toMap(PreferenciaAviso::getTipo, Function.identity(), (a, b) -> b));
        return Arrays.stream(TipoNotificacion.values())
                .map(tipo -> guardadas.getOrDefault(tipo, PreferenciaAviso.porDefecto(usuarioId, tipo)))
                .toList();
    }

    @Override
    public PreferenciaAviso guardarPreferencia(Long usuarioId, TipoNotificacion tipo, CanalAviso canal) {
        if (tipo == null || canal == null) {
            throw new ReglaDeNegocioException("El tipo de aviso y el canal son obligatorios");
        }
        PreferenciaAviso preferencia = preferenciaRepositoryPort.buscar(usuarioId, tipo)
                .orElseGet(() -> PreferenciaAviso.porDefecto(usuarioId, tipo));
        preferencia.cambiarCanal(canal);
        return preferenciaRepositoryPort.guardar(preferencia);
    }
}
