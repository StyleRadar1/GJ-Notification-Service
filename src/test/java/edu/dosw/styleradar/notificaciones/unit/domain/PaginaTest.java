package edu.dosw.styleradar.notificaciones.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginaTest {

    @Test
    @DisplayName("GJ-118 - Calcula el total de paginas y si hay pagina anterior o siguiente")
    void calculaMetadatos() {
        Pagina<String> primera = new Pagina<>(List.of("a", "b", "c", "d", "e"), 0, 5, 12);
        Pagina<String> ultima = new Pagina<>(List.of("k", "l"), 2, 5, 12);

        assertThat(primera.totalPages()).isEqualTo(3);
        assertThat(primera.hasNext()).isTrue();
        assertThat(primera.hasPrevious()).isFalse();
        assertThat(ultima.hasNext()).isFalse();
        assertThat(ultima.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("GJ-118 - Un total exacto no deja una pagina vacia de mas")
    void totalExactoDePaginas() {
        assertThat(new Pagina<String>(List.of(), 0, 5, 10).totalPages()).isEqualTo(2);
        assertThat(new Pagina<String>(List.of(), 0, 5, 11).totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("GJ-118 - Sin elementos hay cero paginas y ninguna pagina anterior ni siguiente")
    void sinElementos() {
        Pagina<String> vacia = new Pagina<>(List.of(), 0, 20, 0);
        Pagina<String> masAlla = new Pagina<>(List.of(), 4, 5, 0);

        assertThat(vacia.totalPages()).isZero();
        assertThat(vacia.hasNext()).isFalse();
        assertThat(vacia.hasPrevious()).isFalse();
        assertThat(masAlla.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("GJ-118 - Un tamano no positivo no produce paginas")
    void tamanoNoPositivo() {
        assertThat(new Pagina<String>(List.of(), 0, 0, 7).totalPages()).isZero();
    }
}
