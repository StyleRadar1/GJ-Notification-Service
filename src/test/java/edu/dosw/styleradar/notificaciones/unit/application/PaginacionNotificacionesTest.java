package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.dto.response.PaginaResponse;
import edu.dosw.styleradar.notificaciones.application.service.impl.NotificacionApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginacionNotificacionesTest {

    private NotificacionRepositoryPort repositorio;
    private NotificacionApplicationServiceImpl servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(NotificacionRepositoryPort.class);
        servicio = new NotificacionApplicationServiceImpl(repositorio);
    }

    @Test
    @DisplayName("GJ-118 - Una pagina valida se pide al repositorio con el mismo filtro y tamano")
    void paginaValidaSeDelega() {
        Pagina<Notificacion> esperada = new Pagina<>(List.of(), 1, 5, 0);
        when(repositorio.listarPaginadas(101L, false, 1, 5)).thenReturn(esperada);

        assertThat(servicio.listarNotificacionesPaginadas(101L, false, 1, 5)).isSameAs(esperada);
        verify(repositorio).listarPaginadas(101L, false, 1, 5);
    }

    @Test
    @DisplayName("GJ-118 - El tamano maximo de 100 es valido")
    void tamanoMaximoEsValido() {
        servicio.listarNotificacionesPaginadas(101L, null, 0, 100);

        verify(repositorio).listarPaginadas(101L, null, 0, 100);
    }

    @Test
    @DisplayName("GJ-118 - Un page negativo o un size fuera de 1 a 100 se rechazan sin consultar el repositorio")
    void parametrosInvalidosSeRechazan() {
        assertThatThrownBy(() -> servicio.listarNotificacionesPaginadas(101L, null, -1, 20))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> servicio.listarNotificacionesPaginadas(101L, null, 0, 0))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> servicio.listarNotificacionesPaginadas(101L, null, 0, 101))
                .isInstanceOf(ReglaDeNegocioException.class);

        verifyNoInteractions(repositorio);
    }

    @Test
    @DisplayName("GJ-118 - La respuesta convierte cada elemento y copia los metadatos de la pagina")
    void respuestaConvierteYCopiaMetadatos() {
        Pagina<Integer> pagina = new Pagina<>(List.of(1, 2, 3, 4, 5), 0, 5, 12);

        PaginaResponse<String> respuesta = PaginaResponse.de(pagina, n -> "n" + n);

        assertThat(respuesta.content()).containsExactly("n1", "n2", "n3", "n4", "n5");
        assertThat(respuesta.page()).isZero();
        assertThat(respuesta.size()).isEqualTo(5);
        assertThat(respuesta.totalElements()).isEqualTo(12);
        assertThat(respuesta.totalPages()).isEqualTo(3);
        assertThat(respuesta.hasNext()).isTrue();
        assertThat(respuesta.hasPrevious()).isFalse();
    }
}
