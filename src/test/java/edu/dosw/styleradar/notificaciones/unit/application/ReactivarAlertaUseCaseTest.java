package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.service.impl.AlertaApplicationServiceImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ActivarAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ConflictoException;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ActivarAlertaUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SR-RF-32 - Reactivar una alerta")
class ReactivarAlertaUseCaseTest {

    private static final long USUARIO = 501L;

    @Mock
    private AlertaRepositoryPort alertas;
    @Mock
    private BusquedaGuardadaCopiaRepositoryPort busquedas;
    @Mock
    private EstadoCuentaCopiaRepositoryPort cuentas;
    @Mock
    private CatalogoClientPort catalogo;
    @Mock
    private EventPublisherPort publicador;

    private ActivarAlertaUseCase activarAlerta;

    @BeforeEach
    void preparar() {
        activarAlerta = new ActivarAlertaUseCaseImpl(new AlertaApplicationServiceImpl(
                alertas, busquedas, cuentas, catalogo, publicador, new AlertaValidator()));
    }

    private Alerta desactivadaDePrenda() {
        return new Alerta(10L, USUARIO, TipoAlerta.ITEM_AGOTADO, "item-1", "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.DESACTIVADA, LocalDateTime.now(), LocalDateTime.now());
    }

    private Alerta desactivadaDeBusqueda() {
        return new Alerta(11L, USUARIO, TipoAlerta.BUSQUEDA_GUARDADA, null, null, 301L,
                CanalNotificacion.IN_APP, EstadoAlerta.DESACTIVADA, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Reactiva una alerta de prenda si sigue agotada y no hay otra activa")
    void reactivaAlertaDePrenda() {
        Alerta alerta = desactivadaDePrenda();
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(alerta));
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.empty());
        when(alertas.existeActivaParaItem(USUARIO, "item-1", "M")).thenReturn(false);
        when(catalogo.verificarItemAgotado("item-1", "M")).thenReturn(true);
        when(alertas.guardar(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        Alerta resultado = activarAlerta.activarAlerta(10L, USUARIO);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
        verify(alertas).guardar(alerta);
    }

    @Test
    @DisplayName("Reactiva una alerta de busqueda si la busqueda sigue activa")
    void reactivaAlertaDeBusqueda() {
        Alerta alerta = desactivadaDeBusqueda();
        when(alertas.buscarPorId(11L)).thenReturn(Optional.of(alerta));
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.empty());
        when(busquedas.buscarPorId(301L)).thenReturn(Optional.of(new BusquedaGuardadaCopia(301L, USUARIO, "Bomber",
                new CriterioBusquedaVO(null, "CHAQUETAS", null, null, null, null, null, null, null), true, LocalDateTime.now())));
        when(alertas.existeActivaParaBusqueda(USUARIO, 301L)).thenReturn(false);
        when(alertas.guardar(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(activarAlerta.activarAlerta(11L, USUARIO).getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
    }

    @Test
    @DisplayName("Una alerta ya activa se devuelve sin cambios ni consultas")
    void yaActivaEsIdempotente() {
        Alerta alerta = desactivadaDePrenda();
        alerta.activar();
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(alerta));

        assertThat(activarAlerta.activarAlerta(10L, USUARIO)).isSameAs(alerta);
        verify(alertas, never()).guardar(any());
        verify(catalogo, never()).verificarItemAgotado(any(), any());
    }

    @Test
    @DisplayName("Rechaza con 409 si ya hay otra alerta activa igual")
    void conflictoConOtraActiva() {
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(desactivadaDePrenda()));
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.empty());
        when(alertas.existeActivaParaItem(USUARIO, "item-1", "M")).thenReturn(true);

        assertThatThrownBy(() -> activarAlerta.activarAlerta(10L, USUARIO)).isInstanceOf(ConflictoException.class);
        verify(alertas, never()).guardar(any());
    }

    @Test
    @DisplayName("Rechaza con 422 si la prenda ya tiene stock o la busqueda ya no esta activa")
    void reglasDeNegocio() {
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(desactivadaDePrenda()));
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(Optional.empty());
        when(alertas.existeActivaParaItem(USUARIO, "item-1", "M")).thenReturn(false);
        when(catalogo.verificarItemAgotado("item-1", "M")).thenReturn(false);
        assertThatThrownBy(() -> activarAlerta.activarAlerta(10L, USUARIO))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageNotContaining("item-1");

        when(alertas.buscarPorId(11L)).thenReturn(Optional.of(desactivadaDeBusqueda()));
        when(busquedas.buscarPorId(301L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> activarAlerta.activarAlerta(11L, USUARIO)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Rechaza con 422 si la cuenta esta bloqueada")
    void cuentaBloqueada() {
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(desactivadaDePrenda()));
        when(cuentas.buscarPorUsuarioId(USUARIO)).thenReturn(
                Optional.of(new EstadoCuentaCopia(USUARIO, "a@b.co", EstadoCuenta.BLOQUEADO, LocalDateTime.now())));

        assertThatThrownBy(() -> activarAlerta.activarAlerta(10L, USUARIO)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Rechaza la alerta ajena con 403 y la inexistente con 404 sin mostrar el id")
    void ajenaEInexistente() {
        when(alertas.buscarPorId(10L)).thenReturn(Optional.of(desactivadaDePrenda()));
        assertThatThrownBy(() -> activarAlerta.activarAlerta(10L, 999L)).isInstanceOf(AccesoDenegadoException.class);

        when(alertas.buscarPorId(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> activarAlerta.activarAlerta(404L, USUARIO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontro la alerta");
    }
}
