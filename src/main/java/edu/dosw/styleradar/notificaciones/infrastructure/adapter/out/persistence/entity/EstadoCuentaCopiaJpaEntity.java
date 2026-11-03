package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity;

import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "estados_cuenta_copia")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCuentaCopiaJpaEntity {

    @Id
    @Column(name = "usuario_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long usuarioId;

    @Column(name = "email", length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoCuenta estado;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
