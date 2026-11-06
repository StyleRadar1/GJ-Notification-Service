package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaRetiradaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.domain.model.Alerta;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.port.in.ProcesarEventosExternosUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba los consumidores contra un RabbitMQ real: el evento viaja por el exchange, llega a su cola,
 * se procesa y, si falla, termina en la cola de errores. Requiere Docker; sin Docker se omite.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "application.messaging.retry.initial-interval-ms=50",
        "application.messaging.retry.max-interval-ms=100"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqEventosIntegrationTest {

    private static final String EXCHANGE = "styleradar.exchange";
    private static final String DLQ = "styleradar.notificaciones.dlq";
    private static final long USUARIO_ID = 9901L;
    private static final long ITEM_ID = 99001L;

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13.7-management-alpine");

    @DynamicPropertySource
    static void configurarBroker(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AlertaRepositoryPort alertaRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private ProcesarEventosExternosUseCase procesarEventosExternosUseCase;

    @AfterEach
    void limpiar() {
        rabbitTemplate.execute(canal -> canal.queuePurge(DLQ));
        jdbcTemplate.update("DELETE FROM notificaciones WHERE usuario_id = ?", USUARIO_ID);
        jdbcTemplate.update("DELETE FROM alertas WHERE usuario_id = ?", USUARIO_ID);
        jdbcTemplate.update("DELETE FROM estados_cuenta_copia WHERE usuario_id = ?", USUARIO_ID);
    }

    @Test
    @DisplayName("NOT-06 - Una reposicion publicada en el broker genera la notificacion de la alerta")
    void reposicionGeneraLaNotificacion() {
        Alerta alerta = crearAlerta();

        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 0, 5, 1));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ciclosNotificados(alerta)).containsExactly(1));
    }

    @Test
    @DisplayName("NOT-06 - Un evento que no es reposicion no genera notificacion")
    void eventoQueNoEsReposicionNoGeneraNotificacion() {
        Alerta alerta = crearAlerta();

        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 5, 8, 1));
        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 0, 5, 2));

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ciclosNotificados(alerta)).containsExactly(2));
    }

    @Test
    @DisplayName("NOT-11 - Un fallo transitorio se reintenta 3 veces y luego llega a la cola de errores")
    void falloTransitorioSeReintentaYLlegaALaDlq() {
        doThrow(new IllegalStateException("base de datos no disponible"))
                .when(procesarEventosExternosUseCase).procesarDisponibilidadPrenda(any());

        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 0, 5, 1));

        assertThat(rabbitTemplate.receive(DLQ, 20_000)).isNotNull();
        verify(procesarEventosExternosUseCase, times(3)).procesarDisponibilidadPrenda(any());
    }

    @Test
    @DisplayName("NOT-11 - Un payload invalido va directo a la cola de errores sin reintentos")
    void payloadInvalidoVaDirectoALaDlq() {
        publicar("styleradar.inventario.actualizado", inventario(null, 0, 5, 1));

        assertThat(rabbitTemplate.receive(DLQ, 20_000)).isNotNull();
        verify(procesarEventosExternosUseCase, never()).procesarDisponibilidadPrenda(any());
    }

    @Test
    @DisplayName("NOT-11 - Un mensaje ilegible va directo a la cola de errores sin reintentos")
    void mensajeIlegibleVaDirectoALaDlq() {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message ilegible = MessageBuilder.withBody("{esto no es json".getBytes(StandardCharsets.UTF_8))
                .andProperties(propiedades).build();

        rabbitTemplate.send(EXCHANGE, "styleradar.inventario.actualizado", ilegible);

        assertThat(rabbitTemplate.receive(DLQ, 20_000)).isNotNull();
        verify(procesarEventosExternosUseCase, never()).procesarDisponibilidadPrenda(any());
    }

    @Test
    @DisplayName("NOT-08 - Una prenda retirada desactiva las alertas del item")
    void prendaRetiradaDesactivaLasAlertas() {
        Alerta alerta = crearAlerta();

        publicar("styleradar.catalogo.prenda.retirada",
                new PrendaRetiradaEventPayload("evt-cat-1", ITEM_ID, "Retirada por la tienda", LocalDateTime.now()));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "SELECT estado FROM alertas WHERE id = ?", String.class, alerta.getId()))
                .isEqualTo("DESACTIVADA"));
    }

    @Test
    @DisplayName("NOT-09 - Una cuenta inactiva no recibe notificaciones y al reactivarse vuelve a recibirlas")
    void cuentaInactivaNoRecibeNotificaciones() {
        Alerta alerta = crearAlerta();

        publicar("styleradar.usuarios.estado.cambiado", estadoCuenta("INACTIVO"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(estadoGuardado()).containsExactly("INACTIVO"));

        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 0, 5, 1));
        verify(procesarEventosExternosUseCase, timeout(20_000)).procesarDisponibilidadPrenda(any());
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ciclosNotificados(alerta)).isEmpty());

        publicar("styleradar.usuarios.estado.cambiado", estadoCuenta("ACTIVO"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(estadoGuardado()).containsExactly("ACTIVO"));

        publicar("styleradar.inventario.actualizado", inventario(ITEM_ID, 0, 5, 2));
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(ciclosNotificados(alerta)).containsExactly(2));
    }

    private void publicar(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
    }

    private Alerta crearAlerta() {
        LocalDateTime ahora = LocalDateTime.now();
        return alertaRepositoryPort.guardar(new Alerta(
                null, USUARIO_ID, TipoAlerta.ITEM_AGOTADO, ITEM_ID, null, null,
                CanalNotificacion.IN_APP, EstadoAlerta.ACTIVA, ahora, ahora));
    }

    private InventarioActualizadoEventPayload inventario(Long itemId, Integer anterior, Integer actual, Integer ciclo) {
        return new InventarioActualizadoEventPayload(
                "evt-inv-" + ciclo, itemId, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", "M", "BogotaStreet",
                "STREETWEAR", new BigDecimal("185000"), anterior, actual, ciclo, 12L, "CHAPINERO", LocalDateTime.now());
    }

    private UsuarioEstadoCambiadoEventPayload estadoCuenta(String estado) {
        return new UsuarioEstadoCambiadoEventPayload(
                "evt-usr-" + estado, USUARIO_ID, "it@styleradar.co", estado, LocalDateTime.now());
    }

    private List<Integer> ciclosNotificados(Alerta alerta) {
        return jdbcTemplate.queryForList(
                "SELECT ciclo_reposicion FROM notificaciones WHERE alerta_id = ? ORDER BY ciclo_reposicion",
                Integer.class, alerta.getId());
    }

    private List<String> estadoGuardado() {
        return jdbcTemplate.queryForList(
                "SELECT estado FROM estados_cuenta_copia WHERE usuario_id = ?", String.class, USUARIO_ID);
    }
}
