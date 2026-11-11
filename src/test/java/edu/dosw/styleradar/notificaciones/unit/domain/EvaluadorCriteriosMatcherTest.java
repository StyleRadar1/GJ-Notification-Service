package edu.dosw.styleradar.notificaciones.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.validator.EvaluadorCriteriosMatcher;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluadorCriteriosMatcherTest {

    private EvaluadorCriteriosMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new EvaluadorCriteriosMatcher();
    }

    @Test
    @DisplayName("NOT-07 - Debe coincidir cuando todos los criterios de la busqueda guardada se cumplen en memoria")
    void debeCoincidirConTodosLosCriterios() {
        // Arrange
        CriterioBusquedaVO criterios = new CriterioBusquedaVO(
                "chaqueta bomber",
                "CHAQUETAS",
                "NEGRO",
                "M",
                "BogotaStreet",
                "STREETWEAR",
                new BigDecimal("100000"),
                new BigDecimal("250000"),
                "CHAPINERO"
        );
        DatosPrendaVO prenda = new DatosPrendaVO(
                1045L,
                "Chaqueta Bomber Oversize",
                "Chaquetas",
                "Negro",
                List.of("S", "M", "L"),
                "BogotaStreet",
                "Streetwear",
                new BigDecimal("185000"),
                5,
                1,
                12L,
                "Chapinero"
        );

        // Act & Assert
        assertThat(matcher.coincideConBusqueda(criterios, prenda)).isTrue();
    }

    @Test
    @DisplayName("NOT-07 - No debe coincidir cuando el stock es cero, el precio excede el rango o difiere algun atributo")
    void noDebeCoincidirCuandoFallaAlgunFiltro() {
        // Arrange
        CriterioBusquedaVO criterios = new CriterioBusquedaVO(
                "bomber", "CHAQUETAS", "NEGRO", "M", "BogotaStreet", "STREETWEAR",
                new BigDecimal("100000"), new BigDecimal("200000"), "CHAPINERO"
        );
        DatosPrendaVO sinStock = new DatosPrendaVO(
                1L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("150000"), 0, 1, 1L, "CHAPINERO"
        );
        DatosPrendaVO cara = new DatosPrendaVO(
                1L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("250000"), 3, 1, 1L, "CHAPINERO"
        );
        DatosPrendaVO barata = new DatosPrendaVO(
                1L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("50000"), 3, 1, 1L, "CHAPINERO"
        );
        DatosPrendaVO otraTalla = new DatosPrendaVO(
                1L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("XL"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("150000"), 3, 1, 1L, "CHAPINERO"
        );
        DatosPrendaVO otroColor = new DatosPrendaVO(
                1L, "Chaqueta Bomber", "CHAQUETAS", "BLANCO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("150000"), 3, 1, 1L, "CHAPINERO"
        );

        // Act & Assert
        assertThat(matcher.coincideConBusqueda(null, cara)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, null)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, sinStock)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, cara)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, barata)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, otraTalla)).isFalse();
        assertThat(matcher.coincideConBusqueda(criterios, otroColor)).isFalse();
    }

    @Test
    @DisplayName("NOT-06 - Debe evaluar correctamente coincidencia de item agotado con o sin talla especifica")
    void debeEvaluarCoincidenciaItemAgotado() {
        // Arrange
        DatosPrendaVO prenda = new DatosPrendaVO(
                1045L, "Pantalon Cargo", "PANTALONES", "VERDE", List.of("32", "34"),
                "UrbanCo", "STREETWEAR", new BigDecimal("140000"), 4, 2, 5L, "USAQUEN"
        );

        // Act & Assert
        assertThat(matcher.coincideItemAgotado(1045L, "32", prenda)).isTrue();
        assertThat(matcher.coincideItemAgotado(1045L, null, prenda)).isTrue();
        assertThat(matcher.coincideItemAgotado(1045L, "38", prenda)).isFalse();
        assertThat(matcher.coincideItemAgotado(9999L, "32", prenda)).isFalse();
    }
}

