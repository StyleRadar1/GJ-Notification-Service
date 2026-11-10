package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.PreferenciaAvisoJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataPreferenciaAvisoRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PreferenciaAvisoRepositoryAdapter implements PreferenciaAvisoRepositoryPort {

    private final SpringDataPreferenciaAvisoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PreferenciaAviso> buscar(Long usuarioId, TipoNotificacion tipo) {
        return repository.findByUsuarioIdAndTipo(usuarioId, tipo).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreferenciaAviso> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreferenciaAviso guardar(PreferenciaAviso preferencia) {
        PreferenciaAvisoJpaEntity entity = repository
                .findByUsuarioIdAndTipo(preferencia.getUsuarioId(), preferencia.getTipo())
                .orElseGet(() -> PreferenciaAvisoJpaEntity.builder()
                        .usuarioId(preferencia.getUsuarioId())
                        .tipo(preferencia.getTipo())
                        .build());
        entity.setCanal(preferencia.getCanal());
        entity.setFechaActualizacion(preferencia.getFechaActualizacion());
        return toDomain(repository.save(entity));
    }

    private PreferenciaAviso toDomain(PreferenciaAvisoJpaEntity entity) {
        return new PreferenciaAviso(entity.getUsuarioId(), entity.getTipo(), entity.getCanal(), entity.getFechaActualizacion());
    }
}
