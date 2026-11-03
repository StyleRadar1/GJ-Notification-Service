package edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "busquedas_guardadas_copia")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaGuardadaCopiaJpaEntity {

    @Id
    @Column(name = "busqueda_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long busquedaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "texto_libre", length = 250)
    private String textoLibre;

    @Column(name = "categoria", length = 80)
    private String categoria;

    @Column(name = "color", length = 60)
    private String color;

    @Column(name = "talla", length = 20)
    private String talla;

    @Column(name = "marca", length = 100)
    private String marca;

    @Column(name = "estilo", length = 80)
    private String estilo;

    @Column(name = "precio_min", precision = 12, scale = 2)
    private BigDecimal precioMin;

    @Column(name = "precio_max", precision = 12, scale = 2)
    private BigDecimal precioMax;

    @Column(name = "localidad", length = 100)
    private String localidad;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "fecha_sincronizacion", nullable = false)
    private LocalDateTime fechaSincronizacion;
}
