package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.usecase.EliminarNotificacionUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Eliminar notificaciones de la bandeja")
class EliminarNotificacionUseCaseTest {

    @Mock
    private NotificacionRepositoryPort repositorio;

    private EliminarNotificacionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EliminarNotificacionUseCaseImpl(repositorio);
    }

    @Test
    @DisplayName("Marca la notificacion propia como eliminada y la guarda")
    void eliminaLaPropia() {
        Notificacion notificacion = notificacion(5L, 42L);
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(notificacion));

        useCase.eliminar(5L, 42L);

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(repositorio).guardar(guardada.capture());
        assertThat(guardada.getValue().estaEliminada()).isTrue();
        assertThat(guardada.getValue().getEliminadaEn()).isNotNull();
    }

    @Test
    @DisplayName("Una notificacion inexistente o ya eliminada no se encuentra")
    void noEncontrada() {
        when(repositorio.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.eliminar(9L, 42L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    @DisplayName("La notificacion de otro usuario se rechaza sin tocarla")
    void deOtroUsuario() {
        when(repositorio.buscarPorId(5L)).thenReturn(Optional.of(notificacion(5L, 7L)));

        assertThatThrownBy(() -> useCase.eliminar(5L, 42L)).isInstanceOf(AccesoDenegadoException.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    @DisplayName("Eliminar las leidas devuelve cuantas elimino el repositorio")
    void eliminaLasLeidas() {
        when(repositorio.eliminarLeidasPorUsuario(42L)).thenReturn(4);

        assertThat(useCase.eliminarLeidas(42L)).isEqualTo(4);
    }

    @Test
    @DisplayName("Eliminar seleccionadas quita los ids repetidos y devuelve lo que borro el repositorio")
    void eliminaSeleccionadas() {
        when(repositorio.eliminarSeleccionadasPorUsuario(42L, java.util.Set.of(5L, 6L))).thenReturn(2);

        assertThat(useCase.eliminarSeleccionadas(42L, java.util.List.of(5L, 6L, 5L))).isEqualTo(2);
    }

    @Test
    @DisplayName("Eliminar seleccionadas rechaza una lista vacia, ids no positivos y mas de 100")
    void rechazaSeleccionesInvalidas() {
        java.util.List<Long> cienUno = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> useCase.eliminarSeleccionadas(42L, java.util.List.of()))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.eliminarSeleccionadas(42L, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.eliminarSeleccionadas(42L, java.util.List.of(0L)))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> useCase.eliminarSeleccionadas(42L, cienUno))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(repositorio, never()).eliminarSeleccionadasPorUsuario(any(), any());
    }

    @Test
    @DisplayName("Marcar como eliminada dos veces conserva la primera fecha")
    void conservaLaPrimeraFecha() {
        Notificacion notificacion = notificacion(5L, 42L);
        notificacion.marcarComoEliminada();
        LocalDateTime primera = notificacion.getEliminadaEn();

        notificacion.marcarComoEliminada();

        assertThat(notificacion.getEliminadaEn()).isEqualTo(primera);
    }

    private Notificacion notificacion(Long id, Long usuarioId) {
        return new Notificacion(id, usuarioId, null, null, null, TipoNotificacion.MENSAJE_NUEVO,
                CanalNotificacion.IN_APP, "Titulo", "Mensaje", "/chat/1", false, LocalDateTime.now(), null);
    }
}
