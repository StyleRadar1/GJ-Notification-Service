package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.BusquedaGuardadaCopiaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataBusquedaGuardadaCopiaRepository
        extends JpaRepository<BusquedaGuardadaCopiaJpaEntity, Long> {
}
