package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.AlertaJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAlertaRepository extends JpaRepository<AlertaJpaEntity, Long> {

    List<AlertaJpaEntity> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<AlertaJpaEntity> findByItemIdAndEstado(Long itemId, EstadoAlerta estado);

    List<AlertaJpaEntity> findByTipoAlertaAndEstado(TipoAlerta tipoAlerta, EstadoAlerta estado);

    List<AlertaJpaEntity> findByBusquedaGuardadaIdAndEstado(Long busquedaGuardadaId, EstadoAlerta estado);

    boolean existsByUsuarioIdAndItemIdAndEstado(Long usuarioId, Long itemId, EstadoAlerta estado);

    boolean existsByUsuarioIdAndItemIdAndTallaObjetivoAndEstado(
            Long usuarioId, Long itemId, String tallaObjetivo, EstadoAlerta estado);

    boolean existsByUsuarioIdAndBusquedaGuardadaIdAndEstado(
            Long usuarioId, Long busquedaGuardadaId, EstadoAlerta estado);

    long countByEstado(EstadoAlerta estado);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AlertaJpaEntity a SET a.estado = :nuevoEstado, a.fechaActualizacion = :fecha " +
           "WHERE a.itemId = :itemId AND a.estado = :estadoActual")
    int actualizarEstadoPorItem(
            @Param("itemId") Long itemId,
            @Param("estadoActual") EstadoAlerta estadoActual,
            @Param("nuevoEstado") EstadoAlerta nuevoEstado,
            @Param("fecha") LocalDateTime fecha
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AlertaJpaEntity a SET a.estado = :nuevoEstado, a.fechaActualizacion = :fecha " +
           "WHERE a.busquedaGuardadaId = :busquedaId AND a.estado = :estadoActual")
    int actualizarEstadoPorBusquedaGuardada(
            @Param("busquedaId") Long busquedaId,
            @Param("estadoActual") EstadoAlerta estadoActual,
            @Param("nuevoEstado") EstadoAlerta nuevoEstado,
            @Param("fecha") LocalDateTime fecha
    );
}
