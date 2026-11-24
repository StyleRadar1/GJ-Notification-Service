package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.NotificacionJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificacionRepository extends JpaRepository<NotificacionJpaEntity, Long> {

    java.util.Optional<NotificacionJpaEntity> findByIdAndEliminadaEnIsNull(Long id);

    List<NotificacionJpaEntity> findByUsuarioIdAndEliminadaEnIsNullOrderByFechaCreacionDesc(Long usuarioId);

    List<NotificacionJpaEntity> findByUsuarioIdAndLeidaAndEliminadaEnIsNullOrderByFechaCreacionDesc(Long usuarioId, boolean leida);

    Page<NotificacionJpaEntity> findByUsuarioIdAndEliminadaEnIsNull(Long usuarioId, Pageable pageable);

    Page<NotificacionJpaEntity> findByUsuarioIdAndLeidaAndEliminadaEnIsNull(Long usuarioId, boolean leida, Pageable pageable);

    List<NotificacionJpaEntity> findByUsuarioIdAndIdGreaterThanAndEliminadaEnIsNullOrderByIdAsc(Long usuarioId, Long id);

    long countByUsuarioIdAndLeidaFalseAndEliminadaEnIsNull(Long usuarioId);

    boolean existsByAlertaIdAndItemIdAndCicloReposicion(Long alertaId, String itemId, Integer cicloReposicion);

    boolean existsByEventoIdAndUsuarioId(String eventoId, Long usuarioId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificacionJpaEntity n SET n.leida = true, n.fechaLectura = :fechaLectura " +
           "WHERE n.usuarioId = :usuarioId AND n.leida = false AND n.eliminadaEn IS NULL")
    int marcarTodasComoLeidas(
            @Param("usuarioId") Long usuarioId,
            @Param("fechaLectura") LocalDateTime fechaLectura
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificacionJpaEntity n SET n.eliminadaEn = :fecha "
            + "WHERE n.usuarioId = :usuarioId AND n.leida = true AND n.eliminadaEn IS NULL")
    int eliminarLeidasPorUsuario(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDateTime fecha);
}
