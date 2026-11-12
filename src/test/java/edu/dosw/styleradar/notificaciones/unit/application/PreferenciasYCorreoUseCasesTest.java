package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.usecase.EnviarAvisoPorCorreoUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.PreferenciasAvisosUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** GJ-64 - Preferencias de canal y decision de enviar el correo. */
@DisplayName("GJ-64 - Preferencias de avisos y envio por correo")
class PreferenciasYCorreoUseCasesTest {

    private static final long USUARIO = 501L;

    private PreferenciaAvisoRepositoryPort preferencias;
    private EstadoCuentaCopiaRepositoryPort cuentas;
    private CorreoPort correo;
    private EnviarAvisoPorCorreoUseCaseImpl enviarCorreo;
    private PreferenciasAvisosUseCaseImpl gestionar;

    @BeforeEach
    void setUp() {
        preferencias = mock(PreferenciaAvisoRepositoryPort.class);
        cuentas = mock(EstadoCuentaCopiaRepositoryPort.class);
        correo = mock(CorreoPort.class);
        enviarCorreo = new EnviarAvisoPorCorreoUseCaseImpl(preferencias, cuentas, correo);
        gestionar = new PreferenciasAvisosUseCaseImpl(preferencias);
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.of(
                new EstadoCuentaCopia(USUARIO, "ana@styleradar.co", EstadoCuenta.ACTIVO, LocalDateTime.now())));
    }

    @Test
    @DisplayName("Con correo y plataforma elegido se envia el correo con el titulo y el mensaje del aviso")
    void conCorreoYPlataformaSeEnvia() {
        when(preferencias.buscar(USUARIO, TipoNotificacion.REPOSICION_STOCK)).thenReturn(
                Optional.of(new PreferenciaAviso(USUARIO, TipoNotificacion.REPOSICION_STOCK, CanalAviso.CORREO_Y_PLATAFORMA, null)));

        enviarCorreo.enviarSiCorresponde(aviso(TipoNotificacion.REPOSICION_STOCK));

        verify(correo).enviar("ana@styleradar.co", "StyleRadar: Prenda disponible", "Volvio a haber stock");
    }

    @Test
    @DisplayName("Con solo plataforma elegido no se envia correo")
    void soloPlataformaNoEnvia() {
        when(preferencias.buscar(USUARIO, TipoNotificacion.REPOSICION_STOCK)).thenReturn(
                Optional.of(new PreferenciaAviso(USUARIO, TipoNotificacion.REPOSICION_STOCK, CanalAviso.SOLO_PLATAFORMA, null)));

        enviarCorreo.enviarSiCorresponde(aviso(TipoNotificacion.REPOSICION_STOCK));

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Sin preferencias guardadas se aplica solo plataforma: no se envia correo")
    void sinPreferenciasNoEnvia() {
        when(preferencias.buscar(USUARIO, TipoNotificacion.MENSAJE_NUEVO)).thenReturn(Optional.empty());

        enviarCorreo.enviarSiCorresponde(aviso(TipoNotificacion.MENSAJE_NUEVO));

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Si el usuario pidio correo pero no hay email guardado, no se envia y no falla")
    void sinEmailNoEnvia() {
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.of(
                new EstadoCuentaCopia(USUARIO, null, EstadoCuenta.ACTIVO, LocalDateTime.now())));
        when(preferencias.buscar(USUARIO, TipoNotificacion.MENSAJE_NUEVO)).thenReturn(
                Optional.of(new PreferenciaAviso(USUARIO, TipoNotificacion.MENSAJE_NUEVO, CanalAviso.CORREO_Y_PLATAFORMA, null)));

        assertThatCode(() -> enviarCorreo.enviarSiCorresponde(aviso(TipoNotificacion.MENSAJE_NUEVO))).doesNotThrowAnyException();

        verify(correo, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Si falla la preparacion del correo, el aviso no se ve afectado: no lanza excepcion")
    void unFalloNuncaSePropaga() {
        when(preferencias.buscar(USUARIO, TipoNotificacion.MENSAJE_NUEVO)).thenReturn(
                Optional.of(new PreferenciaAviso(USUARIO, TipoNotificacion.MENSAJE_NUEVO, CanalAviso.CORREO_Y_PLATAFORMA, null)));
        doThrow(new IllegalStateException("smtp caido")).when(correo).enviar(anyString(), anyString(), anyString());

        assertThatCode(() -> enviarCorreo.enviarSiCorresponde(aviso(TipoNotificacion.MENSAJE_NUEVO))).doesNotThrowAnyException();
        assertThatCode(() -> enviarCorreo.enviarSiCorresponde(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Las preferencias listan todos los tipos, con solo plataforma en los que no se configuraron")
    void obtenerPreferenciasAplicaElValorPorDefecto() {
        when(preferencias.listarPorUsuario(USUARIO)).thenReturn(List.of(
                new PreferenciaAviso(USUARIO, TipoNotificacion.PAGO_CONFIRMADO, CanalAviso.CORREO_Y_PLATAFORMA, null)));

        List<PreferenciaAviso> resultado = gestionar.obtenerPreferencias(USUARIO);

        assertThat(resultado).hasSize(TipoNotificacion.values().length);
        assertThat(resultado).filteredOn(p -> p.getTipo() == TipoNotificacion.PAGO_CONFIRMADO)
                .singleElement().extracting(PreferenciaAviso::getCanal).isEqualTo(CanalAviso.CORREO_Y_PLATAFORMA);
        assertThat(resultado).filteredOn(p -> p.getTipo() != TipoNotificacion.PAGO_CONFIRMADO)
                .allMatch(p -> p.getCanal() == CanalAviso.SOLO_PLATAFORMA);
    }

    @Test
    @DisplayName("Guardar una preferencia nueva o cambiar una existente la persiste con el canal elegido")
    void guardarPreferencia() {
        when(preferencias.buscar(USUARIO, TipoNotificacion.MENSAJE_NUEVO)).thenReturn(Optional.empty());
        when(preferencias.guardar(any(PreferenciaAviso.class))).thenAnswer(inv -> inv.getArgument(0));

        PreferenciaAviso guardada = gestionar.guardarPreferencia(USUARIO, TipoNotificacion.MENSAJE_NUEVO, CanalAviso.CORREO_Y_PLATAFORMA);

        assertThat(guardada.getCanal()).isEqualTo(CanalAviso.CORREO_Y_PLATAFORMA);
        assertThat(guardada.getUsuarioId()).isEqualTo(USUARIO);
    }

    @Test
    @DisplayName("Guardar sin tipo o sin canal es una regla de negocio violada")
    void guardarSinDatosFalla() {
        assertThatThrownBy(() -> gestionar.guardarPreferencia(USUARIO, null, CanalAviso.SOLO_PLATAFORMA))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> gestionar.guardarPreferencia(USUARIO, TipoNotificacion.MENSAJE_NUEVO, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    private Notificacion aviso(TipoNotificacion tipo) {
        Notificacion n = new Notificacion(
                9L, USUARIO, null, null, null, tipo, CanalNotificacion.IN_APP,
                "Prenda disponible", "Volvio a haber stock", "/x", false, LocalDateTime.now(), null);
        return n;
    }
}
