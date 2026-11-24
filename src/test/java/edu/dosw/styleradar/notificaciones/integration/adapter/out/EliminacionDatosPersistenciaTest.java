package edu.dosw.styleradar.notificaciones.integration.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.CanalAviso;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuentaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.EliminarDatosUsuarioUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.PreferenciaAvisoRepositoryPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Eliminacion de cuenta y retiro de prenda contra la base de datos de pruebas. */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Eliminacion de cuenta y retiro de prenda en la base de datos")
class EliminacionDatosPersistenciaTest {

    private static final long USUARIO = 7_100_001L;
    private static final long OTRO_USUARIO = 7_100_002L;

    @Autowired private EliminarDatosUsuarioUseCase eliminarDatosUsuario;
    @Autowired private ProcesarEventosExternosUseCase procesarEventos;
    @Autowired private NotificacionRepositoryPort notificaciones;
    @Autowired private AlertaRepositoryPort alertas;
    @Autowired private PreferenciaAvisoRepositoryPort preferencias;
    @Autowired private CorreoPendienteRepositoryPort correos;
    @Autowired private EstadoCuentaCopiaRepositoryPort estadoCuenta;

    @Test
    @DisplayName("Al eliminar la cuenta se borran sus datos, queda marcada sin correo y los de otros no se tocan")
    void eliminarCuenta() {
        estadoCuenta.guardar(new EstadoCuentaCopia(USUARIO, "u@sr.co", EstadoCuenta.ACTIVO, LocalDateTime.now()));
        notificaciones.guardar(notificacion(USUARIO, TipoNotificacion.MENSAJE_NUEVO, null, "/chat/1", false));
        notificaciones.guardar(notificacion(OTRO_USUARIO, TipoNotificacion.MENSAJE_NUEVO, null, "/chat/2", false));
        alertas.guardar(alerta(USUARIO, "7100010"));
        alertas.guardar(alerta(OTRO_USUARIO, "7100011"));
        preferencias.guardar(new PreferenciaAviso(
                USUARIO, TipoNotificacion.MENSAJE_NUEVO, CanalAviso.SOLO_PLATAFORMA, LocalDateTime.now()));
        correos.guardar(CorreoPendiente.nuevo(USUARIO, "u@sr.co", "Asunto", "Cuerpo", 3, "fallo"));

        eliminarDatosUsuario.eliminarDatosDeUsuario(USUARIO);

        assertThat(notificaciones.listarPorUsuario(USUARIO, null)).isEmpty();
        assertThat(alertas.listarPorUsuario(USUARIO)).isEmpty();
        assertThat(preferencias.listarPorUsuario(USUARIO)).isEmpty();
        assertThat(correos.listarPendientes(1000)).noneMatch(c -> USUARIO == c.getUsuarioId());
        EstadoCuentaCopia copia = estadoCuenta.buscarPorUsuarioId(USUARIO).orElseThrow();
        assertThat(copia.getEstado()).isEqualTo(EstadoCuenta.ELIMINADA);
        assertThat(copia.getEmail()).isNull();

        assertThat(notificaciones.listarPorUsuario(OTRO_USUARIO, null)).hasSize(1);
        assertThat(alertas.listarPorUsuario(OTRO_USUARIO)).hasSize(1);
    }

    @Test
    @DisplayName("Repetir la eliminacion no falla")
    void esIdempotente() {
        eliminarDatosUsuario.eliminarDatosDeUsuario(USUARIO + 50);
        eliminarDatosUsuario.eliminarDatosDeUsuario(USUARIO + 50);

        assertThat(estadoCuenta.buscarPorUsuarioId(USUARIO + 50).orElseThrow().getEstado())
                .isEqualTo(EstadoCuenta.ELIMINADA);
    }

    @Test
    @DisplayName("Una cuenta eliminada ya no recibe notificaciones directas")
    void cuentaEliminadaNoRecibe() {
        eliminarDatosUsuario.eliminarDatosDeUsuario(USUARIO + 60);

        Notificacion generada = procesarEventos.generarNotificacionDirecta(
                USUARIO + 60, TipoNotificacion.MENSAJE_NUEVO, "Chat", "Hola", "/chat/9", (String) null);

        assertThat(generada).isNull();
        assertThat(notificaciones.listarPorUsuario(USUARIO + 60, null)).isEmpty();
    }

    @Test
    @DisplayName("Al retirar una prenda se borran las no leidas de disponibilidad, a las leidas se les quita el "
            + "enlace y la moderacion queda intacta")
    void retirarPrenda() {
        String item = "prenda-7100099";
        long usuario = USUARIO + 70;
        Notificacion noLeida = notificaciones.guardar(
                notificacion(usuario, TipoNotificacion.REPOSICION_STOCK, item, "/catalogo/prendas/" + item, false));
        Notificacion leida = notificaciones.guardar(notificacion(
                usuario, TipoNotificacion.COINCIDENCIA_BUSQUEDA, item, "/catalogo/prendas/" + item, true));
        Notificacion moderacion = notificaciones.guardar(notificacion(
                usuario, TipoNotificacion.MODERACION_PRENDA, item, "/catalogo/mis-prendas/" + item, false));
        Notificacion otraPrenda = notificaciones.guardar(notificacion(
                usuario, TipoNotificacion.REPOSICION_STOCK, "otra-7100098", "/catalogo/prendas/otra-7100098", false));
        alertas.guardar(alerta(usuario, item));

        procesarEventos.desactivarAlertasPorItemEliminado(item);

        assertThat(notificaciones.buscarPorId(noLeida.getId())).isEmpty();
        assertThat(notificaciones.buscarPorId(leida.getId()).orElseThrow().getEnlace()).isNull();
        assertThat(notificaciones.buscarPorId(moderacion.getId()).orElseThrow().getEnlace())
                .isEqualTo("/catalogo/mis-prendas/" + item);
        assertThat(notificaciones.buscarPorId(otraPrenda.getId()).orElseThrow().getEnlace())
                .isEqualTo("/catalogo/prendas/otra-7100098");
        assertThat(alertas.listarPorUsuario(usuario)).allMatch(a -> a.getEstado() == EstadoAlerta.DESACTIVADA);
    }

    private Notificacion notificacion(
            Long usuario, TipoNotificacion tipo, String itemId, String enlace, boolean leida) {
        return new Notificacion(null, usuario, null, itemId, null, tipo, CanalNotificacion.IN_APP,
                "Titulo", "Mensaje", enlace, leida, LocalDateTime.now(), leida ? LocalDateTime.now() : null);
    }

    private Alerta alerta(Long usuario, String itemId) {
        return new Alerta(null, usuario, TipoAlerta.ITEM_AGOTADO, itemId, null, null, CanalNotificacion.IN_APP,
                EstadoAlerta.ACTIVA, LocalDateTime.now(), LocalDateTime.now());
    }
}
