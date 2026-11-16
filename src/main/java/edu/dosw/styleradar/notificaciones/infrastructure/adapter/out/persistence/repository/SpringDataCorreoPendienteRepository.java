package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity.CorreoPendienteJpaEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataCorreoPendienteRepository extends JpaRepository<CorreoPendienteJpaEntity, Long> {

    @Query("SELECT c FROM CorreoPendienteJpaEntity c WHERE c.estado = :estado AND c.intentos < c.maxIntentos ORDER BY c.id ASC")
    List<CorreoPendienteJpaEntity> buscarReintentables(@Param("estado") EstadoCorreo estado, Pageable pageable);
}
