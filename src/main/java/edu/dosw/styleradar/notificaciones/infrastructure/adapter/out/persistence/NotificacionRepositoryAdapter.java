package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.mapper.NotificacionEntityMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.NotificacionJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataNotificacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificacionRepositoryAdapter implements NotificacionRepositoryPort {

    private final SpringDataNotificacionRepository repository;
    private final NotificacionEntityMapper mapper;

    // Sin transaccion propia: cada operacion del repositorio abre la suya. Si la restriccion unica
    // (alerta, item, ciclo) rechaza el duplicado de una entrega simultanea, solo se revierte esa
    // escritura y no una transaccion exterior que despues fallaria con UnexpectedRollbackException.
    @Override
    public Notificacion guardar(Notificacion notificacion) {
        if (notificacion.getId() == null
                && notificacion.getAlertaId() != null
                && notificacion.getItemId() != null
                && notificacion.getCicloReposicion() != null
                && repository.existsByAlertaIdAndItemIdAndCicloReposicion(
                        notificacion.getAlertaId(),
                        notificacion.getItemId(),
                        notificacion.getCicloReposicion())) {
            return notificacion;
        }
        try {
            NotificacionJpaEntity entity = mapper.toEntity(notificacion);
            NotificacionJpaEntity guardada = repository.saveAndFlush(entity);
            return mapper.toDomain(guardada);
        } catch (DataIntegrityViolationException ex) {
            return notificacion;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notificacion> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId, Boolean soloNoLeidas) {
        List<NotificacionJpaEntity> entities = Boolean.TRUE.equals(soloNoLeidas)
                ? repository.findByUsuarioIdAndLeidaOrderByFechaCreacionDesc(usuarioId, false)
                : repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId) {
        return repository.findByUsuarioIdAndIdGreaterThanOrderByIdAsc(usuarioId, ultimoId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidasPorUsuario(Long usuarioId) {
        return repository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int marcarTodasComoLeidasPorUsuario(Long usuarioId) {
        return repository.marcarTodasComoLeidas(usuarioId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorAlertaItemYCiclo(Long alertaId, Long itemId, Integer cicloReposicion) {
        return repository.existsByAlertaIdAndItemIdAndCicloReposicion(alertaId, itemId, cicloReposicion);
    }
}

