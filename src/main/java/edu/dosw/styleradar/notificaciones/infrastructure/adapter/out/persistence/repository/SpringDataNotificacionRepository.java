package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.NotificacionJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificacionRepository extends JpaRepository<NotificacionJpaEntity, Long> {

    List<NotificacionJpaEntity> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<NotificacionJpaEntity> findByUsuarioIdAndLeidaOrderByFechaCreacionDesc(Long usuarioId, boolean leida);

    List<NotificacionJpaEntity> findByUsuarioIdAndIdGreaterThanOrderByIdAsc(Long usuarioId, Long id);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    boolean existsByAlertaIdAndItemIdAndCicloReposicion(Long alertaId, Long itemId, Integer cicloReposicion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificacionJpaEntity n SET n.leida = true, n.fechaLectura = :fechaLectura " +
           "WHERE n.usuarioId = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(
            @Param("usuarioId") Long usuarioId,
            @Param("fechaLectura") LocalDateTime fechaLectura
    );
}
