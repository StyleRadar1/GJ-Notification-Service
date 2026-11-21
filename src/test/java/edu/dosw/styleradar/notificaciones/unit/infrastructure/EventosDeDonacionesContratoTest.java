package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaVendidaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.SolicitudDonacionCreadaEventPayload;
import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitMqConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Contrato con ms-segunda-vida-donaciones: nombres de campo alternativos de la solicitud, la venta sin interesados
 * y las dos routing keys de la prenda vendida.
 */
@DisplayName("Eventos de Donaciones: alias de campos y routing keys")
class EventosDeDonacionesContratoTest {

    private final Jackson2JsonMessageConverter convertidor = new Jackson2JsonMessageConverter();

    private <T> T leer(String json, Class<T> tipo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message mensaje = new Message(json.getBytes(StandardCharsets.UTF_8), propiedades);
        return tipo.cast(convertidor.fromMessage(mensaje, ParameterizedTypeReference.forType(tipo)));
    }

    @Test
    @DisplayName("La solicitud acepta representanteId y solicitudId, e ignora fundacionId y donanteId")
    void solicitudConNombresDeDonaciones() {
        SolicitudDonacionCreadaEventPayload evento = leer(
                "{\"eventoId\":\"e1\",\"solicitudId\":40,\"representanteId\":55,\"donanteId\":9,\"fundacionId\":3,"
                        + "\"nombreDonante\":\"Ana\",\"fechaEvento\":\"2026-11-18T10:30:00\"}",
                SolicitudDonacionCreadaEventPayload.class);

        assertThat(evento.donacionId()).isEqualTo(40L);
        assertThat(evento.fundacionUsuarioId()).isEqualTo(55L);
        assertThat(evento.nombreDonante()).isEqualTo("Ana");
        assertThat(evento.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 18, 10, 30));
    }

    @Test
    @DisplayName("La solicitud con los nombres originales sigue funcionando")
    void solicitudConNombresOriginales() {
        SolicitudDonacionCreadaEventPayload evento = leer(
                "{\"eventoId\":\"e2\",\"donacionId\":41,\"fundacionUsuarioId\":56,\"nombreDonante\":\"Luis\"}",
                SolicitudDonacionCreadaEventPayload.class);

        assertThat(evento.donacionId()).isEqualTo(41L);
        assertThat(evento.fundacionUsuarioId()).isEqualTo(56L);
    }

    @Test
    @DisplayName("La venta con vendedorId e interesadosIds vacio se lee sin error")
    void ventaSinInteresados() {
        PrendaVendidaEventPayload evento = leer(
                "{\"eventoId\":\"v1\",\"publicacionId\":77,\"vendedorId\":4,\"compradorId\":8,\"interesadosIds\":[]}",
                PrendaVendidaEventPayload.class);

        assertThat(evento.publicacionId()).isEqualTo("77");
        assertThat(evento.vendedorId()).isEqualTo(4L);
        assertThat(evento.compradorId()).isEqualTo(8L);
        assertThat(evento.interesadosIds()).isEmpty();
    }

    @Test
    @DisplayName("La cola de prenda vendida queda vinculada a las dos routing keys")
    void dosBindingsParaLaPrendaVendida() {
        RabbitMqConfig config = new RabbitMqConfig();
        ReflectionTestUtils.setField(config, "garmentSoldKey", "styleradar.segundavida.prenda.vendida");
        ReflectionTestUtils.setField(config, "garmentSoldAltKey", "styleradar.segunda-mano.prenda.vendida");
        Queue cola = new Queue("styleradar.notificaciones.prenda-vendida.q");
        TopicExchange exchange = new TopicExchange("styleradar.exchange");

        Binding principal = config.garmentSoldBinding(cola, exchange);
        Binding alterna = config.garmentSoldAltBinding(cola, exchange);

        assertThat(principal.getRoutingKey()).isEqualTo("styleradar.segundavida.prenda.vendida");
        assertThat(alterna.getRoutingKey()).isEqualTo("styleradar.segunda-mano.prenda.vendida");
        assertThat(alterna.getDestination()).isEqualTo("styleradar.notificaciones.prenda-vendida.q");
        assertThat(alterna.getExchange()).isEqualTo("styleradar.exchange");
    }
}
