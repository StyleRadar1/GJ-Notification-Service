package edu.dosw.styleradar.notificaciones.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoFeignClient;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * GJ-63 - Al crear una alerta, el servicio publica AlertaCreada en el exchange compartido con la
 * routing key acordada con Estadisticas. Se comprueba contra un RabbitMQ real: una cola de prueba se
 * enlaza a esa routing key y se lee el mensaje. Requiere Docker; sin Docker se omite.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("GJ-63 - Publicacion del evento AlertaCreada en RabbitMQ")
class AlertaCreadaPublicacionIntegrationTest {

    private static final String EXCHANGE = "styleradar.exchange";
    private static final String ROUTING_KEY = "styleradar.notificaciones.alerta.creada";
    private static final String COLA_DE_PRUEBA = "test.estadisticas.alerta-creada.q";
    private static final long USUARIO_ID = 63_001L;
    private static final long ITEM_ID = 63_002L;

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CatalogoFeignClient catalogoFeignClient;

    @BeforeEach
    void enlazarColaDePrueba() {
        Queue cola = new Queue(COLA_DE_PRUEBA, false, false, true);
        rabbitAdmin.declareQueue(cola);
        Binding enlace = BindingBuilder.bind(cola).to(new TopicExchange(EXCHANGE, true, false)).with(ROUTING_KEY);
        rabbitAdmin.declareBinding(enlace);
        rabbitAdmin.purgeQueue(COLA_DE_PRUEBA, false);
    }

    @AfterEach
    void limpiar() {
        rabbitAdmin.deleteQueue(COLA_DE_PRUEBA);
        jdbcTemplate.update("DELETE FROM alertas WHERE usuario_id = ?", USUARIO_ID);
    }

    @Test
    @DisplayName("Crear una alerta publica AlertaCreada con su contrato en styleradar.exchange")
    void crearUnaAlertaPublicaElEvento() throws Exception {
        when(catalogoFeignClient.consultarDisponibilidad(ITEM_ID))
                .thenReturn(new CatalogoDisponibilidadItemResponse(ITEM_ID, 0, List.of()));
        String token = jwtTokenService.generateToken(USUARIO_ID, "estadisticas@styleradar.co", "COMPRADOR");

        String respuesta = mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":" + ITEM_ID + ",\"canal\":\"IN_APP\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long alertaId = objectMapper.readTree(respuesta).get("id").asLong();

        Message mensaje = rabbitTemplate.receive(COLA_DE_PRUEBA, 15_000);

        assertThat(mensaje).as("el evento debe llegar a la cola enlazada a la routing key").isNotNull();
        assertThat(mensaje.getMessageProperties().getReceivedRoutingKey()).isEqualTo(ROUTING_KEY);
        JsonNode evento = objectMapper.readTree(mensaje.getBody());
        assertThat(evento.get("eventoId").asText()).isNotBlank();
        assertThat(evento.get("alertaId").asLong()).isEqualTo(alertaId);
        assertThat(evento.get("usuarioId").asLong()).isEqualTo(USUARIO_ID);
        assertThat(evento.get("tipoAlerta").asText()).isEqualTo("ITEM_AGOTADO");
        assertThat(evento.get("itemId").asLong()).isEqualTo(ITEM_ID);
        assertThat(evento.get("canal").asText()).isEqualTo("IN_APP");
        assertThat(evento.hasNonNull("fechaCreacion")).isTrue();
    }

    @Test
    @DisplayName("Si la alerta no se crea (prenda con stock), no se publica ningun evento")
    void sinAlertaNoHayEvento() throws Exception {
        when(catalogoFeignClient.consultarDisponibilidad(ITEM_ID))
                .thenReturn(new CatalogoDisponibilidadItemResponse(ITEM_ID, 7, List.of("M")));
        String token = jwtTokenService.generateToken(USUARIO_ID, "estadisticas@styleradar.co", "COMPRADOR");

        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoAlerta\":\"ITEM_AGOTADO\",\"itemId\":" + ITEM_ID + ",\"canal\":\"IN_APP\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(rabbitTemplate.receive(COLA_DE_PRUEBA, 2_000)).isNull();
    }
}
