package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.dosw.styleradar.notificaciones.application.usecase.CrearAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.DesactivarAlertaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarAlertasUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ListarNotificacionesUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.MarcarNotificacionLeidaUseCaseImpl;
import edu.dosw.styleradar.notificaciones.application.usecase.ProcesarEventosExternosUseCaseImpl;
import edu.dosw.styleradar.notificaciones.domain.event.AlertaCreadaDomainEvent;
import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ConflictoException;
import edu.dosw.styleradar.notificaciones.domain.exception.RecursoNoEncontradoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CatalogoClientPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EventPublisherPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.RealTimeNotificationPort;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import edu.dosw.styleradar.notificaciones.domain.validator.EvaluadorCriteriosMatcher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacionesUseCasesTest {

    @Mock
    private AlertaRepositoryPort alertaRepositoryPort;
    @Mock
    private NotificacionRepositoryPort notificacionRepositoryPort;
    @Mock
    private BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort;
    @Mock
    private EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;
    @Mock
    private CatalogoClientPort catalogoClientPort;
    @Mock
    private EventPublisherPort eventPublisherPort;
    @Mock
    private RealTimeNotificationPort realTimeNotificationPort;

    private CrearAlertaUseCaseImpl crearAlertaUseCase;
    private ListarAlertasUseCaseImpl listarAlertasUseCase;
    private DesactivarAlertaUseCaseImpl desactivarAlertaUseCase;
    private ListarNotificacionesUseCaseImpl listarNotificacionesUseCase;
    private MarcarNotificacionLeidaUseCaseImpl marcarNotificacionLeidaUseCase;
    private ProcesarEventosExternosUseCaseImpl procesarEventosExternosUseCase;

    @BeforeEach
    void setUp() {
        AlertaValidator validator = new AlertaValidator();
        EvaluadorCriteriosMatcher matcher = new EvaluadorCriteriosMatcher();

        crearAlertaUseCase = new CrearAlertaUseCaseImpl(
                alertaRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                catalogoClientPort,
                eventPublisherPort,
                validator
        );
        listarAlertasUseCase = new ListarAlertasUseCaseImpl(alertaRepositoryPort);
        desactivarAlertaUseCase = new DesactivarAlertaUseCaseImpl(alertaRepositoryPort, validator);
        listarNotificacionesUseCase = new ListarNotificacionesUseCaseImpl(notificacionRepositoryPort);
        marcarNotificacionLeidaUseCase = new MarcarNotificacionLeidaUseCaseImpl(notificacionRepositoryPort);
        procesarEventosExternosUseCase = new ProcesarEventosExternosUseCaseImpl(
                alertaRepositoryPort,
                notificacionRepositoryPort,
                busquedaGuardadaCopiaRepositoryPort,
                estadoCuentaCopiaRepositoryPort,
                realTimeNotificationPort,
                matcher
        );
    }

    @Test
    @DisplayName("NOT-03, NOT-10 y NOT-16 - Debe crear alerta de item agotado y publicar evento AlertaCreada")
    void debeCrearAlertaItemAgotadoYPublicarEvento() {
        // Arrange
        Alerta nueva = new Alerta(null, 501L, TipoAlerta.ITEM_AGOTADO, 1045L, "M", null, null, null, null, null);
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(501L)).thenReturn(Optional.empty());
        when(alertaRepositoryPort.existeActivaParaItem(501L, 1045L, "M")).thenReturn(false);
        when(catalogoClientPort.verificarItemAgotado(1045L, "M")).thenReturn(true);
        when(alertaRepositoryPort.guardar(any(Alerta.class))).thenAnswer(inv -> {
            Alerta arg = inv.getArgument(0);
            arg.setId(77L);
            return arg;
        });

        // Act
        Alerta creada = crearAlertaUseCase.crearAlerta(nueva);

        // Assert
        assertThat(creada.getId()).isEqualTo(77L);
        assertThat(creada.getCanal()).isEqualTo(CanalNotificacion.IN_APP);
        assertThat(creada.getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
        verify(eventPublisherPort).publicarAlertaCreada(any(AlertaCreadaDomainEvent.class));
    }

    @Test
    @DisplayName("NOT-03 y NOT-08 - Debe crear alerta de busqueda guardada activa")
    void debeCrearAlertaBusquedaGuardada() {
        // Arrange
        Alerta nueva = new Alerta(null, 501L, TipoAlerta.BUSQUEDA_GUARDADA, null, null, 300L, CanalNotificacion.EMAIL, null, null, null);
        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia(300L, 501L, "Chaquetas", null, true, LocalDateTime.now());
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(501L)).thenReturn(Optional.empty());
        when(busquedaGuardadaCopiaRepositoryPort.buscarPorId(300L)).thenReturn(Optional.of(copia));
        when(alertaRepositoryPort.existeActivaParaBusqueda(501L, 300L)).thenReturn(false);
        when(alertaRepositoryPort.guardar(any(Alerta.class))).thenAnswer(inv -> {
            Alerta arg = inv.getArgument(0);
            arg.setId(88L);
            return arg;
        });

        // Act
        Alerta creada = crearAlertaUseCase.crearAlerta(nueva);

        // Assert
        assertThat(creada.getId()).isEqualTo(88L);
        assertThat(creada.getCanal()).isEqualTo(CanalNotificacion.EMAIL);
    }

    @Test
    @DisplayName("NOT-10 - Debe rechazar alerta si el item tiene stock disponible en catalogo o si ya existe alerta activa")
    void debeRechazarItemConStockODuplicado() {
        // Arrange
        Alerta nueva = new Alerta(null, 501L, TipoAlerta.ITEM_AGOTADO, 1045L, "M", null, null, null, null, null);
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(501L)).thenReturn(Optional.empty());
        when(alertaRepositoryPort.existeActivaParaItem(501L, 1045L, "M")).thenReturn(false);
        when(catalogoClientPort.verificarItemAgotado(1045L, "M")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> crearAlertaUseCase.crearAlerta(nueva))
                .isInstanceOf(ReglaDeNegocioException.class);

        when(alertaRepositoryPort.existeActivaParaItem(501L, 1045L, "M")).thenReturn(true);
        assertThatThrownBy(() -> crearAlertaUseCase.crearAlerta(nueva))
                .isInstanceOf(ConflictoException.class);
        verify(alertaRepositoryPort, never()).guardar(any());
    }

    @Test
    @DisplayName("NOT-03 - Debe listar y desactivar alertas validando propiedad del usuario")
    void debeListarYDesactivarAlertas() {
        // Arrange
        Alerta existente = new Alerta(10L, 501L, TipoAlerta.ITEM_AGOTADO, 1045L, "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now());
        when(alertaRepositoryPort.listarPorUsuario(501L)).thenReturn(List.of(existente));
        when(alertaRepositoryPort.buscarPorId(10L)).thenReturn(Optional.of(existente));
        when(alertaRepositoryPort.guardar(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        List<Alerta> lista = listarAlertasUseCase.listarAlertasPorUsuario(501L);
        Alerta desactivada = desactivarAlertaUseCase.desactivarAlerta(10L, 501L);

        // Assert
        assertThat(lista).hasSize(1);
        assertThat(desactivada.getEstado()).isEqualTo(EstadoAlerta.DESACTIVADA);

        assertThatThrownBy(() -> desactivarAlertaUseCase.desactivarAlerta(10L, 999L))
                .isInstanceOf(AccesoDenegadoException.class);
        when(alertaRepositoryPort.buscarPorId(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> desactivarAlertaUseCase.desactivarAlerta(404L, 501L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("NOT-13 - Debe listar notificaciones, contar no leidas y marcar individual o todas como leidas")
    void debeGestionarLecturaDeNotificaciones() {
        // Arrange
        Notificacion notif = new Notificacion(5L, 501L, 10L, 1045L, 1, TipoNotificacion.REPOSICION_STOCK,
                CanalNotificacion.IN_APP, "Titulo", "Mensaje", "/catalogo/prendas/1045", false, LocalDateTime.now(), null);
        when(notificacionRepositoryPort.listarPorUsuario(501L, true)).thenReturn(List.of(notif));
        when(notificacionRepositoryPort.contarNoLeidasPorUsuario(501L)).thenReturn(3L);
        when(notificacionRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(notif));
        when(notificacionRepositoryPort.guardar(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepositoryPort.marcarTodasComoLeidasPorUsuario(501L)).thenReturn(3);

        // Act
        List<Notificacion> noLeidas = listarNotificacionesUseCase.listarNotificacionesPorUsuario(501L, true);
        long conteo = listarNotificacionesUseCase.contarNoLeidas(501L);
        Notificacion leida = marcarNotificacionLeidaUseCase.marcarComoLeida(5L, 501L);
        int marcadas = marcarNotificacionLeidaUseCase.marcarTodasComoLeidas(501L);

        // Assert
        assertThat(noLeidas).hasSize(1);
        assertThat(conteo).isEqualTo(3L);
        assertThat(leida.isLeida()).isTrue();
        assertThat(marcadas).isEqualTo(3);

        assertThatThrownBy(() -> marcarNotificacionLeidaUseCase.marcarComoLeida(5L, 888L))
                .isInstanceOf(AccesoDenegadoException.class);
        when(notificacionRepositoryPort.buscarPorId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> marcarNotificacionLeidaUseCase.marcarComoLeida(999L, 501L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("NOT-06, NOT-07, NOT-09 y NOT-11 - Debe procesar reposicion y coincidencia de busqueda respetando idempotencia y estado de cuenta")
    void debeProcesarDisponibilidadPrendaConIdempotenciaYEstadoCuenta() {
        // Arrange
        DatosPrendaVO prenda = new DatosPrendaVO(
                1045L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("180000"), 5, 2, 12L, "CHAPINERO"
        );
        Alerta alertaItem = new Alerta(1L, 501L, TipoAlerta.ITEM_AGOTADO, 1045L, "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now());
        Alerta alertaInactivo = new Alerta(2L, 502L, TipoAlerta.ITEM_AGOTADO, 1045L, "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now());
        Alerta alertaBusqueda = new Alerta(3L, 503L, TipoAlerta.BUSQUEDA_GUARDADA, null, null, 300L,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now());

        CriterioBusquedaVO criterios = new CriterioBusquedaVO(
                "bomber", "CHAQUETAS", "NEGRO", "M", null, null, null, null, "CHAPINERO"
        );
        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia(300L, 503L, "Mis Chaquetas", criterios, true, LocalDateTime.now());

        when(alertaRepositoryPort.listarActivasPorItem(1045L)).thenReturn(List.of(alertaItem, alertaInactivo));
        when(alertaRepositoryPort.listarActivasPorTipo(TipoAlerta.BUSQUEDA_GUARDADA)).thenReturn(List.of(alertaBusqueda));
        when(busquedaGuardadaCopiaRepositoryPort.buscarPorId(300L)).thenReturn(Optional.of(copia));

        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(501L))
                .thenReturn(Optional.of(new EstadoCuentaCopia(501L, "u1@sr.co", EstadoCuenta.ACTIVO, LocalDateTime.now())));
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(502L))
                .thenReturn(Optional.of(new EstadoCuentaCopia(502L, "u2@sr.co", EstadoCuenta.INACTIVO, LocalDateTime.now())));
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(503L)).thenReturn(Optional.empty());

        when(notificacionRepositoryPort.existePorAlertaItemYCiclo(1L, 1045L, 2)).thenReturn(false);
        when(notificacionRepositoryPort.existePorAlertaItemYCiclo(3L, 1045L, 2)).thenReturn(false);
        when(notificacionRepositoryPort.guardar(any(Notificacion.class))).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(99L);
            return n;
        });

        // Act
        List<Notificacion> generadas = procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prenda);

        // Assert
        assertThat(generadas).hasSize(2);
        verify(realTimeNotificationPort, times(2)).emitirNotificacionTiempoReal(any(), any());
    }

    @Test
    @DisplayName("NOT-08, NOT-09 y NOT-14 - Debe sincronizar busquedas, estados de cuenta y notificaciones directas")
    void debeSincronizarCopiasYNotificacionesDirectas() {
        // Arrange
        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia(300L, 501L, "Busqueda", null, true, LocalDateTime.now());
        when(estadoCuentaCopiaRepositoryPort.buscarPorUsuarioId(501L)).thenReturn(Optional.empty());
        when(notificacionRepositoryPort.guardar(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        procesarEventosExternosUseCase.sincronizarBusquedaGuardada(copia, false);
        procesarEventosExternosUseCase.sincronizarBusquedaGuardada(copia, true);
        procesarEventosExternosUseCase.desactivarAlertasPorItemEliminado(1045L);
        procesarEventosExternosUseCase.actualizarEstadoCuenta(501L, "u@sr.co", EstadoCuenta.ACTIVO);
        Notificacion directa = procesarEventosExternosUseCase.generarNotificacionDirecta(
                501L, TipoNotificacion.MENSAJE_NUEVO, "Chat", "Hola", "/chat/1", null);

        // Assert
        assertThat(directa).isNotNull();
        assertThat(directa.getTipo()).isEqualTo(TipoNotificacion.MENSAJE_NUEVO);
        verify(alertaRepositoryPort).desactivarPorBusquedaGuardada(300L);
        verify(alertaRepositoryPort).desactivarPorItem(1045L);
        verify(estadoCuentaCopiaRepositoryPort).guardar(any(EstadoCuentaCopia.class));
    }
}
