package edu.dosw.styleradar.notificaciones.integration.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doReturn;

import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.persistence.repository.SpringDataNotificacionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NotificacionesConcurrenciaTest {

    private static final long USUARIO_ID = 8801L;
    private static final long ITEM_ID = 88001L;

    @Autowired
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @Autowired
    private AlertaRepositoryPort alertaRepositoryPort;

    @Autowired
    private NotificacionRepositoryPort notificacionRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private SpringDataNotificacionRepository notificacionRepository;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM notificaciones WHERE usuario_id = ?", USUARIO_ID);
        jdbcTemplate.update("DELETE FROM alertas WHERE usuario_id = ?", USUARIO_ID);
    }

    @Test
    @DisplayName("NOT-11 - Entregas simultaneas del mismo evento generan una sola notificacion y ningun fallo")
    void entregasSimultaneasGeneranUnaSolaNotificacion() throws Exception {
        Alerta alerta = crearAlertaActiva();
        DatosPrendaVO prenda = prendaConCiclo(2);

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<?>> entregas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            entregas.add(pool.submit(() -> {
                salida.await();
                procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prenda);
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> entrega : entregas) {
            entrega.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(contarNotificaciones(alerta.getId(), 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("NOT-11 - Un evento repetido en secuencia no duplica la notificacion y un ciclo nuevo si genera otra")
    void eventoRepetidoNoDuplicaYCicloNuevoGeneraOtra() {
        Alerta alerta = crearAlertaActiva();

        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prendaConCiclo(1));
        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prendaConCiclo(1));
        assertThat(contarNotificaciones(alerta.getId(), 1)).isEqualTo(1);

        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prendaConCiclo(2));
        assertThat(contarNotificaciones(alerta.getId(), 2)).isEqualTo(1);
    }

    @Test
    @DisplayName("NOT-11 - Quien pierde la carrera por el mismo ciclo descarta el duplicado sin fallar ni notificar")
    void perdedorDeLaCarreraDescartaElDuplicadoSinFallar() {
        Alerta alerta = crearAlertaActiva();
        procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prendaConCiclo(5));
        assertThat(contarNotificaciones(alerta.getId(), 5)).isEqualTo(1);

        // Simula la entrega que ya paso la verificacion previa cuando la otra todavia no habia guardado
        doReturn(false).when(notificacionRepository)
                .existsByAlertaIdAndItemIdAndCicloReposicion(alerta.getId(), ITEM_ID, 5);

        Notificacion duplicada = new Notificacion(null, USUARIO_ID, alerta.getId(), ITEM_ID, 5,
                TipoNotificacion.REPOSICION_STOCK, CanalNotificacion.IN_APP, "Titulo", "Mensaje",
                "/catalogo/prendas/" + ITEM_ID, false, LocalDateTime.now(), null);

        Notificacion resultado = assertDoesNotThrow(() -> notificacionRepositoryPort.guardar(duplicada));
        assertThat(resultado.getId()).isNull();
        assertThat(procesarEventosExternosUseCase.procesarDisponibilidadPrenda(prendaConCiclo(5))).isEmpty();
        assertThat(contarNotificaciones(alerta.getId(), 5)).isEqualTo(1);
    }

    private Alerta crearAlertaActiva() {
        LocalDateTime ahora = LocalDateTime.now();
        return alertaRepositoryPort.guardar(new Alerta(
                null, USUARIO_ID, TipoAlerta.ITEM_AGOTADO, ITEM_ID, null, null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, ahora, ahora));
    }

    private DatosPrendaVO prendaConCiclo(int ciclo) {
        return new DatosPrendaVO(ITEM_ID, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", List.of("M"),
                "BogotaStreet", "STREETWEAR", new BigDecimal("185000"), 8, ciclo, 12L, "CHAPINERO");
    }

    private int contarNotificaciones(Long alertaId, int ciclo) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notificaciones WHERE alerta_id = ? AND item_id = ? AND ciclo_reposicion = ?",
                Integer.class, alertaId, ITEM_ID, ciclo);
        return total == null ? 0 : total;
    }
}
