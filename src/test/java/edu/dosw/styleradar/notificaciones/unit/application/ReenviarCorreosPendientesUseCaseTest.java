package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.usecase.ReenviarCorreosPendientesUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EnvioCorreoPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReenviarCorreosPendientesUseCaseTest {

    private CorreoPendienteRepositoryPort repositorio;
    private EnvioCorreoPort envio;
    private ReenviarCorreosPendientesUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repositorio = mock(CorreoPendienteRepositoryPort.class);
        envio = mock(EnvioCorreoPort.class);
        when(envio.estaDisponible()).thenReturn(true);
        useCase = new ReenviarCorreosPendientesUseCaseImpl(repositorio, envio);
    }

    private CorreoPendiente pendiente(String destinatario, int maxIntentos) {
        return CorreoPendiente.nuevo(7L, destinatario, "Asunto", "Cuerpo", maxIntentos, "origen");
    }

    @Test
    @DisplayName("GJ-119 - Un correo que ahora si sale se marca ENVIADO y se guarda")
    void correoEnviadoSeMarca() {
        CorreoPendiente correo = pendiente("ana@styleradar.co", 3);
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of(correo));

        int enviados = useCase.reenviarPendientes();

        assertThat(enviados).isEqualTo(1);
        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        verify(envio).enviarSincrono("ana@styleradar.co", "Asunto", "Cuerpo");
        verify(repositorio).guardar(correo);
    }

    @Test
    @DisplayName("GJ-119 - Si el reintento falla se cuenta el intento y el correo sigue PENDIENTE")
    void falloSumaUnIntento() {
        CorreoPendiente correo = pendiente("ana@styleradar.co", 3);
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of(correo));
        doThrow(new ServicioExternoNoDisponibleException("smtp caido")).when(envio).enviarSincrono(any(), any(), any());

        int enviados = useCase.reenviarPendientes();

        assertThat(enviados).isZero();
        assertThat(correo.getIntentos()).isEqualTo(1);
        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(correo.getUltimoError()).contains("smtp caido");
        verify(repositorio).guardar(correo);
    }

    @Test
    @DisplayName("GJ-119 - Al llegar al maximo de intentos el correo queda FALLIDO")
    void alAlcanzarElMaximoQuedaFallido() {
        CorreoPendiente correo = pendiente("ana@styleradar.co", 1);
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of(correo));
        doThrow(new ServicioExternoNoDisponibleException("smtp caido")).when(envio).enviarSincrono(any(), any(), any());

        useCase.reenviarPendientes();

        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.FALLIDO);
        assertThat(correo.getIntentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("GJ-119 - Un correo que falla no impide enviar los demas y se cuentan solo los enviados")
    void unFalloNoDetieneElLote() {
        CorreoPendiente malo = pendiente("malo@styleradar.co", 3);
        CorreoPendiente bueno = pendiente("bueno@styleradar.co", 3);
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of(malo, bueno));
        doThrow(new ServicioExternoNoDisponibleException("rechazado")).when(envio)
                .enviarSincrono("malo@styleradar.co", "Asunto", "Cuerpo");

        int enviados = useCase.reenviarPendientes();

        assertThat(enviados).isEqualTo(1);
        assertThat(malo.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(bueno.getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        verify(repositorio, times(2)).guardar(any());
    }

    @Test
    @DisplayName("GJ-119 - Sin servidor de correo no se reintenta nada ni se gastan intentos")
    void sinServidorNoHaceNada() {
        when(envio.estaDisponible()).thenReturn(false);

        assertThat(useCase.reenviarPendientes()).isZero();

        verifyNoInteractions(repositorio);
        verify(envio, never()).enviarSincrono(any(), any(), any());
    }

    @Test
    @DisplayName("GJ-119 - Sin correos pendientes no hay nada que enviar")
    void sinPendientes() {
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of());

        assertThat(useCase.reenviarPendientes()).isZero();

        verify(envio, never()).enviarSincrono(any(), any(), any());
    }

    @Test
    @DisplayName("GJ-119 - Un correo que ya no es reintentable que llega en la lista se salta")
    void correoNoReintentableSeSalta() {
        CorreoPendiente yaEnviado = pendiente("ana@styleradar.co", 3);
        yaEnviado.marcarEnviado();
        when(repositorio.listarPendientes(anyInt())).thenReturn(List.of(yaEnviado));

        assertThat(useCase.reenviarPendientes()).isZero();

        verify(envio, never()).enviarSincrono(any(), any(), any());
        verify(repositorio, never()).guardar(any());
    }
}
