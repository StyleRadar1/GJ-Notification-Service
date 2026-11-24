package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.PreferenciaAvisoJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPreferenciaAvisoRepository extends JpaRepository<PreferenciaAvisoJpaEntity, Long> {

    Optional<PreferenciaAvisoJpaEntity> findByUsuarioIdAndTipo(Long usuarioId, TipoNotificacion tipo);

    List<PreferenciaAvisoJpaEntity> findByUsuarioId(Long usuarioId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PreferenciaAvisoJpaEntity e WHERE e.usuarioId = :usuarioId")
    int eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
