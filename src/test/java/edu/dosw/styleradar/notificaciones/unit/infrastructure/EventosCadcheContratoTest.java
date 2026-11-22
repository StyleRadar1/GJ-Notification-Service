package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionDesactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.FundacionReactivadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitMqConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Contrato con los microservicios CADCHE v2 (ms-usuarios y ms-tiendas): alias de campos, valores reales en QA
 * y marcas de tiempo ISO-8601 con sufijo Z.
 */
@DisplayName("Eventos CADCHE v2: alias y fechas UTC")
class EventosCadcheContratoTest {

    private final Jackson2JsonMessageConverter convertidor =
            (Jackson2JsonMessageConverter) new RabbitMqConfig().jackson2JsonMessageConverter();

    private <T> T leer(String json, Class<T> tipo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message mensaje = new Message(json.getBytes(StandardCharsets.UTF_8), propiedades);
        return tipo.cast(convertidor.fromMessage(mensaje, ParameterizedTypeReference.forType(tipo)));
    }

    @Test
    @DisplayName("UsuarioEstadoCambiado acepta el JSON real de ms-usuarios v2 (estadoNuevo y fechaCambio con Z)")
    void usuarioEstadoCambiadoFormatoCadche() {
        String json = """
                {
                  "usuarioId": 12,
                  "estadoAnterior": "ACTIVA",
                  "estadoNuevo": "BLOQUEADA",
                  "administradorId": 1,
                  "motivo": "Incumplimiento de terminos",
                  "fechaCambio": "2026-11-21T22:30:00Z"
                }
                """;

        UsuarioEstadoCambiadoEventPayload evento = leer(json, UsuarioEstadoCambiadoEventPayload.class);

        assertThat(evento.usuarioId()).isEqualTo(12L);
        assertThat(evento.nuevoEstado()).isEqualTo("BLOQUEADA");
        assertThat(evento.fechaCambio()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 30));
    }

    @Test
    @DisplayName("UsuarioEstadoCambiado acepta el JSON v2 en ingles acordado con ms-usuarios")
    void usuarioEstadoCambiadoFormatoInglesV2() {
        String json = """
                {
                  "eventId": "evt-usr-8802",
                  "userId": 12,
                  "email": "usuario@styleradar.co",
                  "previousStatus": "ACTIVE",
                  "newStatus": "BLOCKED",
                  "changedBy": 1,
                  "reason": "Incumplimiento de terminos",
                  "changedAt": "2026-11-21T23:55:00Z"
                }
                """;

        UsuarioEstadoCambiadoEventPayload evento = leer(json, UsuarioEstadoCambiadoEventPayload.class);

        assertThat(evento.eventoId()).isEqualTo("evt-usr-8802");
        assertThat(evento.usuarioId()).isEqualTo(12L);
        assertThat(evento.email()).isEqualTo("usuario@styleradar.co");
        assertThat(evento.nuevoEstado()).isEqualTo("BLOCKED");
        assertThat(evento.fechaCambio()).isEqualTo(LocalDateTime.of(2026, 11, 21, 23, 55));
    }

    @Test
    @DisplayName("FundacionDesactivada acepta el JSON exacto de ms-tiendas v2 (ingles + deactivatedAt con Z)")
    void fundacionDesactivadaFormatoCadche() {
        String json = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "foundationId": 3,
                  "foundationName": "Fundación Abrigo Solidario",
                  "representativeId": 24,
                  "deactivatedAt": "2026-11-21T22:30:00Z",
                  "reason": "Cierre temporal de operaciones"
                }
                """;

        FundacionDesactivadaEventPayload evento = leer(json, FundacionDesactivadaEventPayload.class);

        assertThat(evento.eventoId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(evento.fundacionId()).isEqualTo(3L);
        assertThat(evento.nombreFundacion()).isEqualTo("Fundación Abrigo Solidario");
        assertThat(evento.representanteId()).isEqualTo(24L);
        assertThat(evento.donantesAfectadosIds()).isNull();
        assertThat(evento.motivo()).isEqualTo("Cierre temporal de operaciones");
        assertThat(evento.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 30));
    }

    @Test
    @DisplayName("FundacionDesactivada con nombres originales en espanol sigue funcionando igual")
    void fundacionDesactivadaFormatoOriginal() {
        String json = """
                {
                  "eventoId": "evt-fund-9901",
                  "fundacionId": 3,
                  "nombreFundacion": "Fundación Abrigo Solidario",
                  "donantesAfectadosIds": [501, 502],
                  "motivo": "Cambio de sede",
                  "fechaEvento": "2026-11-16T10:00:00"
                }
                """;

        FundacionDesactivadaEventPayload evento = leer(json, FundacionDesactivadaEventPayload.class);

        assertThat(evento.eventoId()).isEqualTo("evt-fund-9901");
        assertThat(evento.donantesAfectadosIds()).containsExactly(501L, 502L);
        assertThat(evento.representanteId()).isNull();
        assertThat(evento.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 16, 10, 0));
    }

    @Test
    @DisplayName("FundacionReactivada acepta el JSON exacto de ms-tiendas v2 (foundation.reactivated)")
    void fundacionReactivadaFormatoCadche() {
        String json = """
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440001",
                  "foundationId": 3,
                  "foundationName": "Fundación Abrigo Solidario",
                  "representativeId": 24,
                  "reactivatedAt": "2026-11-21T22:35:00Z",
                  "reason": "Reapertura de recepción de donaciones"
                }
                """;

        FundacionReactivadaEventPayload evento = leer(json, FundacionReactivadaEventPayload.class);

        assertThat(evento.eventoId()).isEqualTo("550e8400-e29b-41d4-a716-446655440001");
        assertThat(evento.fundacionId()).isEqualTo(3L);
        assertThat(evento.nombreFundacion()).isEqualTo("Fundación Abrigo Solidario");
        assertThat(evento.representanteId()).isEqualTo(24L);
        assertThat(evento.motivo()).isEqualTo("Reapertura de recepción de donaciones");
        assertThat(evento.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 22, 35));
    }
}
