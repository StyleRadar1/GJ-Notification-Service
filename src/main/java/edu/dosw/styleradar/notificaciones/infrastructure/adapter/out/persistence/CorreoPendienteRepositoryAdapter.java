package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence;

import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.CorreoPendienteJpaEntity;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataCorreoPendienteRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CorreoPendienteRepositoryAdapter implements CorreoPendienteRepositoryPort {

    private final SpringDataCorreoPendienteRepository repository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CorreoPendiente guardar(CorreoPendiente correo) {
        CorreoPendienteJpaEntity entity = toEntity(correo);
        CorreoPendiente guardado = toDomain(repository.save(entity));
        correo.setId(guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorreoPendiente> buscarPorId(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorreoPendiente> listarPendientes(int limite) {
        return repository.buscarReintentables(EstadoCorreo.PENDIENTE, PageRequest.of(0, Math.max(1, limite)))
                .stream().map(this::toDomain).toList();
    }

    private CorreoPendienteJpaEntity toEntity(CorreoPendiente correo) {
        return CorreoPendienteJpaEntity.builder()
                .id(correo.getId())
                .usuarioId(correo.getUsuarioId())
                .destinatario(correo.getDestinatario())
                .asunto(correo.getAsunto())
                .cuerpo(correo.getCuerpo())
                .intentos(correo.getIntentos())
                .maxIntentos(correo.getMaxIntentos())
                .estado(correo.getEstado())
                .ultimoError(correo.getUltimoError())
                .fechaCreacion(correo.getFechaCreacion())
                .fechaActualizacion(correo.getFechaActualizacion())
                .build();
    }

    private CorreoPendiente toDomain(CorreoPendienteJpaEntity entity) {
        return new CorreoPendiente(
                entity.getId(), entity.getUsuarioId(), entity.getDestinatario(), entity.getAsunto(),
                entity.getCuerpo(), entity.getIntentos(), entity.getMaxIntentos(), entity.getEstado(),
                entity.getUltimoError(), entity.getFechaCreacion(), entity.getFechaActualizacion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int eliminarPorUsuario(Long usuarioId) {
        return repository.eliminarPorUsuario(usuarioId);
    }
}
