package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.dosw.styleradar.notificaciones.application.usecase.EliminarDatosUsuarioUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cuenta eliminada: borrado de los datos de notificaciones del usuario")
class EliminarDatosUsuarioUseCaseTest {

    @Mock private EstadoCuentaCopiaRepositoryPort estadoCuenta;
    @Mock private NotificacionRepositoryPort notificaciones;
    @Mock private AlertaRepositoryPort alertas;
    @Mock private PreferenciaAvisoRepositoryPort preferencias;
    @Mock private CorreoPendienteRepositoryPort correos;
    @Mock private BusquedaGuardadaCopiaRepositoryPort busquedas;
    @Mock private RealTimeNotificationPort tiempoReal;

    private EliminarDatosUsuarioUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EliminarDatosUsuarioUseCaseImpl(
                estadoCuenta, notificaciones, alertas, preferencias, correos, busquedas, tiempoReal);
    }

    @Test
    @DisplayName("Marca la cuenta como eliminada sin correo y borra todos sus datos")
    void borraTodo() {
        useCase.eliminarDatosDeUsuario(42L);

        ArgumentCaptor<EstadoCuentaCopia> copia = ArgumentCaptor.forClass(EstadoCuentaCopia.class);
        verify(estadoCuenta).guardar(copia.capture());
        assertThat(copia.getValue().getUsuarioId()).isEqualTo(42L);
        assertThat(copia.getValue().getEstado()).isEqualTo(EstadoCuenta.ELIMINADA);
        assertThat(copia.getValue().getEmail()).isNull();
        assertThat(copia.getValue().puedeRecibirNotificaciones()).isFalse();

        verify(tiempoReal).cerrarConexionesDeUsuario(42L);
        verify(notificaciones).eliminarPorUsuario(42L);
        verify(alertas).eliminarPorUsuario(42L);
        verify(preferencias).eliminarPorUsuario(42L);
        verify(correos).eliminarPorUsuario(42L);
        verify(busquedas).eliminarPorUsuario(42L);
    }

    @Test
    @DisplayName("Primero marca la cuenta y despues borra, para que un evento tardio no le genere avisos")
    void marcaAntesDeBorrar() {
        useCase.eliminarDatosDeUsuario(42L);

        InOrder orden = inOrder(estadoCuenta, notificaciones, alertas);
        orden.verify(estadoCuenta).guardar(any(EstadoCuentaCopia.class));
        orden.verify(notificaciones).eliminarPorUsuario(42L);
        orden.verify(alertas).eliminarPorUsuario(42L);
    }

    @Test
    @DisplayName("Sin usuarioId no hace nada")
    void sinUsuario() {
        useCase.eliminarDatosDeUsuario(null);

        verifyNoInteractions(estadoCuenta, notificaciones, alertas, preferencias, correos, busquedas, tiempoReal);
    }
}
