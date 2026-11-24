package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.mapper.NotificacionEntityMapper;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.NotificacionJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataNotificacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return repository.findByIdAndEliminadaEnIsNull(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> listarPorUsuario(Long usuarioId, Boolean soloNoLeidas) {
        List<NotificacionJpaEntity> entities = Boolean.TRUE.equals(soloNoLeidas)
                ? repository.findByUsuarioIdAndLeidaAndEliminadaEnIsNullOrderByFechaCreacionDesc(usuarioId, false)
                : repository.findByUsuarioIdAndEliminadaEnIsNullOrderByFechaCreacionDesc(usuarioId);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Notificacion> listarPaginadas(Long usuarioId, Boolean leida, int page, int size) {
        // Mas recientes primero; el id desempata las que se crearon en el mismo instante
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id")));
        Page<NotificacionJpaEntity> resultado = leida == null
                ? repository.findByUsuarioIdAndEliminadaEnIsNull(usuarioId, pageable)
                : repository.findByUsuarioIdAndLeidaAndEliminadaEnIsNull(usuarioId, leida, pageable);
        return new Pagina<>(
                resultado.getContent().stream().map(mapper::toDomain).toList(),
                page,
                size,
                resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> listarPosterioresA(Long usuarioId, Long ultimoId) {
        return repository.findByUsuarioIdAndIdGreaterThanAndEliminadaEnIsNullOrderByIdAsc(usuarioId, ultimoId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidasPorUsuario(Long usuarioId) {
        return repository.countByUsuarioIdAndLeidaFalseAndEliminadaEnIsNull(usuarioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int marcarTodasComoLeidasPorUsuario(Long usuarioId) {
        return repository.marcarTodasComoLeidas(usuarioId, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int eliminarLeidasPorUsuario(Long usuarioId) {
        return repository.eliminarLeidasPorUsuario(usuarioId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorAlertaItemYCiclo(Long alertaId, String itemId, Integer cicloReposicion) {
        return repository.existsByAlertaIdAndItemIdAndCicloReposicion(alertaId, itemId, cicloReposicion);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorEventoYUsuario(String eventoId, Long usuarioId) {
        return repository.existsByEventoIdAndUsuarioId(eventoId, usuarioId);
    }
}

