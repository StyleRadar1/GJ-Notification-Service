package edu.dosw.styleradar.notificaciones.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ConflictoException;
import edu.dosw.styleradar.notificaciones.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.validator.AlertaValidator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertaValidatorAndModelTest {

    private AlertaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AlertaValidator();
    }

    @Test
    @DisplayName("NOT-02 - Debe validar exitosamente una alerta de item agotado para cuenta activa")
    void debeValidarAlertaItemAgotadoValida() {
        // Arrange
        Alerta alerta = new Alerta(
                1L, 100L, TipoAlerta.ITEM_AGOTADO, 50L, "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now()
        );
        EstadoCuentaCopia cuenta = new EstadoCuentaCopia(100L, "user@styleradar.co", EstadoCuenta.ACTIVO, LocalDateTime.now());

        // Act
        validator.validarCreacionAlerta(alerta, cuenta);
        validator.validarItemAgotadoEnCatalogo(true, 50L);
        validator.validarNoDuplicada(false);
        validator.validarPropiedadAlerta(alerta, 100L);

        // Assert
        assertThat(alerta.estaActiva()).isTrue();
    }

    @Test
    @DisplayName("NOT-02 - Debe rechazar alerta nula, sin usuario o sin tipo")
    void debeRechazarAlertaIncompleta() {
        // Arrange
        Alerta sinUsuario = new Alerta();
        Alerta sinTipo = new Alerta();
        sinTipo.setUsuarioId(10L);

        // Act & Assert
        assertThatThrownBy(() -> validator.validarCreacionAlerta(null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarCreacionAlerta(sinUsuario, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarCreacionAlerta(sinTipo, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("NOT-09 - Debe rechazar creacion de alerta si la cuenta del usuario esta inactiva o bloqueada")
    void debeRechazarCuentaInactiva() {
        // Arrange
        Alerta alerta = new Alerta(
                null, 100L, TipoAlerta.ITEM_AGOTADO, 50L, "M", null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, null, null
        );
        EstadoCuentaCopia cuentaInactiva = new EstadoCuentaCopia(100L, "u@sr.co", EstadoCuenta.INACTIVO, LocalDateTime.now());

        // Act & Assert
        assertThatThrownBy(() -> validator.validarCreacionAlerta(alerta, cuentaInactiva))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("INACTIVO");
    }

    @Test
    @DisplayName("NOT-02 - Debe rechazar combinacion invalida de itemId y busquedaGuardadaId")
    void debeRechazarCombinacionesMutuamenteExcluyentes() {
        // Arrange
        Alerta itemSinId = new Alerta(null, 10L, TipoAlerta.ITEM_AGOTADO, null, "L", null, null, null, null, null);
        Alerta itemConBusqueda = new Alerta(null, 10L, TipoAlerta.ITEM_AGOTADO, 5L, "L", 9L, null, null, null, null);
        Alerta busquedaSinId = new Alerta(null, 10L, TipoAlerta.BUSQUEDA_GUARDADA, null, null, null, null, null, null, null);
        Alerta busquedaConItem = new Alerta(null, 10L, TipoAlerta.BUSQUEDA_GUARDADA, 5L, null, 9L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> validator.validarCreacionAlerta(itemSinId, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarCreacionAlerta(itemConBusqueda, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarCreacionAlerta(busquedaSinId, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarCreacionAlerta(busquedaConItem, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("NOT-10 - Debe rechazar alerta de item agotado cuando el catalogo reporta stock disponible")
    void debeRechazarItemConStockEnCatalogo() {
        assertThatThrownBy(() -> validator.validarItemAgotadoEnCatalogo(false, 99L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("NOT-08 - Debe validar busqueda guardada activa y pertenencia al usuario")
    void debeValidarBusquedaGuardadaYPropiedad() {
        // Arrange
        CriterioBusquedaVO criterios = new CriterioBusquedaVO("bomber", "CHAQUETAS", "NEGRO", "M", null, null,
                new BigDecimal("50000"), new BigDecimal("250000"), "CHAPINERO");
        BusquedaGuardadaCopia activa = new BusquedaGuardadaCopia(1L, 10L, "Mi Busqueda", criterios, true, LocalDateTime.now());
        BusquedaGuardadaCopia inactiva = new BusquedaGuardadaCopia(2L, 10L, "Inactiva", criterios, false, LocalDateTime.now());

        // Act & Assert
        validator.validarBusquedaGuardadaActiva(activa, 10L);
        assertThatThrownBy(() -> validator.validarBusquedaGuardadaActiva(null, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarBusquedaGuardadaActiva(inactiva, 10L))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> validator.validarBusquedaGuardadaActiva(activa, 999L))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("NOT-02 - Debe lanzar ConflictoException en duplicados y AccesoDenegadoException en propiedad ajena")
    void debeRechazarDuplicadosYPropiedadAjena() {
        Alerta alerta = new Alerta(1L, 10L, TipoAlerta.ITEM_AGOTADO, 5L, "M", null, null, null, null, null);
        assertThatThrownBy(() -> validator.validarNoDuplicada(true))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> validator.validarPropiedadAlerta(alerta, 20L))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    @DisplayName("NOT-12 y NOT-13 - Debe gestionar transiciones de estado en Alerta, Notificacion y copias")
    void debeGestionarModelosDeDominio() {
        // Arrange
        Alerta alerta = new Alerta();
        alerta.setId(1L);
        alerta.setUsuarioId(2L);
        alerta.setTipoAlerta(TipoAlerta.ITEM_AGOTADO);
        alerta.setItemId(3L);
        alerta.setTallaObjetivo("S");
        alerta.setBusquedaGuardadaId(null);
        alerta.setCanal(CanalNotificacion.EMAIL);
        alerta.setEstado(EstadoAlerta.ACTIVA);
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setFechaActualizacion(LocalDateTime.now());

        Notificacion notificacion = new Notificacion();
        notificacion.setId(10L);
        notificacion.setUsuarioId(2L);
        notificacion.setAlertaId(1L);
        notificacion.setItemId(3L);
        notificacion.setCicloReposicion(1);
        notificacion.setTipo(TipoNotificacion.REPOSICION_STOCK);
        notificacion.setCanal(CanalNotificacion.IN_APP);
        notificacion.setTitulo("Stock");
        notificacion.setMensaje("Disponible");
        notificacion.setEnlace("/catalogo/prendas/3");
        notificacion.setLeida(false);
        notificacion.setFechaCreacion(LocalDateTime.now());

        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia();
        copia.setBusquedaId(7L);
        copia.setUsuarioId(2L);
        copia.setNombre("Streetwear");
        copia.setActiva(true);
        copia.setFechaSincronizacion(LocalDateTime.now());

        EstadoCuentaCopia estadoCuenta = new EstadoCuentaCopia();
        estadoCuenta.setUsuarioId(2L);
        estadoCuenta.setEmail("test@sr.co");
        estadoCuenta.setEstado(EstadoCuenta.BLOQUEADO);
        estadoCuenta.setFechaActualizacion(LocalDateTime.now());

        // Act
        alerta.marcarDisparada();
        assertThat(alerta.getEstado()).isEqualTo(EstadoAlerta.DISPARADA);
        alerta.desactivar();
        assertThat(alerta.estaActiva()).isFalse();

        notificacion.marcarComoLeida();
        LocalDateTime primeraLectura = notificacion.getFechaLectura();
        notificacion.marcarComoLeida();

        // Assert
        assertThat(notificacion.isLeida()).isTrue();
        assertThat(notificacion.getFechaLectura()).isEqualTo(primeraLectura);
        assertThat(notificacion.getEnlace()).isEqualTo("/catalogo/prendas/3");
        assertThat(copia.getBusquedaId()).isEqualTo(7L);
        assertThat(estadoCuenta.puedeRecibirNotificaciones()).isFalse();
    }
}

