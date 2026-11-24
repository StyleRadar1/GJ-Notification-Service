package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.BusquedaGuardadaCopiaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBusquedaGuardadaCopiaRepository
        extends JpaRepository<BusquedaGuardadaCopiaJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM BusquedaGuardadaCopiaJpaEntity e WHERE e.usuarioId = :usuarioId")
    int eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
