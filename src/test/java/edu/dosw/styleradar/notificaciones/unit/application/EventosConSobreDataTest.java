package edu.dosw.styleradar.notificaciones.unit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PagoConfirmadoEventPayload;
import edu.dosw.styleradar.notificaciones.infrastructure.config.FlexibleLocalDateTimeDeserializer;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Eventos de Inventory y Payment con sobre data")
class EventosConSobreDataTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .addModule(new SimpleModule().addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer()))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    @DisplayName("Inventory: lee prendaId, sedeId y stockDisponible desde data")
    void inventarioConSobre() throws Exception {
        String json = """
                {"eventoId":"e-1","eventId":"e-1","eventType":"styleradar.inventario.actualizado",
                 "timestamp":"2026-11-21T15:30:00Z","producer":"ms-inventario",
                 "data":{"prendaId":"7f3b2c1e-0000-4000-8000-000000000001","sedeId":"sede-9","talla":"M",
                         "stockAnterior":0,"stockDisponible":5,"stockReservado":0,"estado":"ACTUALIZADO"}}
                """;

        InventarioActualizadoEventPayload p = mapper.readValue(json, InventarioActualizadoEventPayload.class);

        assertThat(p.eventoId()).isEqualTo("e-1");
        assertThat(p.itemId()).isEqualTo("7f3b2c1e-0000-4000-8000-000000000001");
        assertThat(p.tiendaId()).isEqualTo("sede-9");
        assertThat(p.talla()).isEqualTo("M");
        assertThat(p.stockAnterior()).isZero();
        assertThat(p.stockActual()).isEqualTo(5);
        assertThat(p.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 15, 30));
        assertThat(p.nombrePrenda()).isNull();
        assertThat(p.esReposicion()).isTrue();
    }

    @Test
    @DisplayName("Inventory: el formato plano anterior sigue funcionando")
    void inventarioPlano() throws Exception {
        String json = """
                {"eventoId":"e-2","itemId":"item-1","nombrePrenda":"Chaqueta","talla":"S",
                 "stockAnterior":0,"stockActual":3,"tiendaId":"t-1","fechaEvento":"2026-11-21T10:00:00"}
                """;

        InventarioActualizadoEventPayload p = mapper.readValue(json, InventarioActualizadoEventPayload.class);

        assertThat(p.itemId()).isEqualTo("item-1");
        assertThat(p.nombrePrenda()).isEqualTo("Chaqueta");
        assertThat(p.stockActual()).isEqualTo(3);
        assertThat(p.fechaEvento()).isEqualTo(LocalDateTime.of(2026, 11, 21, 10, 0));
    }

    @Test
    @DisplayName("Payment: lee ordenId y usuarioId desde data y arma el plan con planId")
    void pagoConSobre() throws Exception {
        String json = """
                {"eventId":"0b8e5d9a-0000-4000-8000-000000000002","eventType":"styleradar.pagos.pago.confirmado",
                 "version":1,"timestamp":"2026-11-21T16:00:00Z","producer":"ms-pagos-suscripciones",
                 "data":{"ordenId":77,"suscriptorId":"42","usuarioId":42,"tipoSuscriptor":"COMPRADOR","planId":3,"estado":"CONFIRMADO"}}
                """;

        PagoConfirmadoEventPayload p = mapper.readValue(json, PagoConfirmadoEventPayload.class);

        assertThat(p.eventoId()).isEqualTo("0b8e5d9a-0000-4000-8000-000000000002");
        assertThat(p.pagoId()).isEqualTo(77L);
        assertThat(p.usuarioId()).isEqualTo(42L);
        assertThat(p.plan()).isEqualTo("#3");
        assertThat(p.fechaConfirmacion()).isEqualTo(LocalDateTime.of(2026, 11, 21, 16, 0));
    }

    @Test
    @DisplayName("Payment: si solo llega suscriptorId numerico se usa como usuario")
    void pagoSoloSuscriptor() throws Exception {
        String json = "{\"eventId\":\"x\",\"data\":{\"ordenId\":1,\"suscriptorId\":\"99\"}}";

        PagoConfirmadoEventPayload p = mapper.readValue(json, PagoConfirmadoEventPayload.class);

        assertThat(p.usuarioId()).isEqualTo(99L);
        assertThat(p.plan()).isNull();
    }

    @Test
    @DisplayName("Payment: el formato plano anterior sigue funcionando")
    void pagoPlano() throws Exception {
        String json = "{\"eventoId\":\"e-3\",\"pagoId\":5,\"usuarioId\":7,\"plan\":\"Radar+\","
                + "\"fechaConfirmacion\":\"2026-11-21T09:00:00\"}";

        PagoConfirmadoEventPayload p = mapper.readValue(json, PagoConfirmadoEventPayload.class);

        assertThat(p.usuarioId()).isEqualTo(7L);
        assertThat(p.plan()).isEqualTo("Radar+");
    }

    @Test
    @DisplayName("Las fechas se serializan como texto ISO y no como arreglo")
    void fechasComoIso() throws Exception {
        String json = mapper.writeValueAsString(
                new PagoConfirmadoEventPayload("e", 1L, 2L, "p", LocalDateTime.of(2026, 11, 21, 9, 0)));

        assertThat(json).contains("\"fechaConfirmacion\":\"2026-11-21T09:00:00\"");
    }
}
