package edu.dosw.styleradar.notificaciones.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FlexibleLocalDateTimeDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    record Wrapper(@JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime fecha) {}

    @Test
    void deserializaIsoSinZonaConZYEpochNumerico() throws Exception {
        Wrapper sinZona = mapper.readValue("{\"fecha\":\"2026-11-27T05:10:00\"}", Wrapper.class);
        assertThat(sinZona.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 27, 5, 10, 0));

        Wrapper conZ = mapper.readValue("{\"fecha\":\"2026-11-27T05:10:00Z\"}", Wrapper.class);
        assertThat(conZ.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 27, 5, 10, 0));

        Wrapper epochFloat = mapper.readValue("{\"fecha\":1795756200.500000000}", Wrapper.class);
        assertThat(epochFloat.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 27, 5, 10, 0, 500_000_000));

        Wrapper epochInt = mapper.readValue("{\"fecha\":1795756200}", Wrapper.class);
        assertThat(epochInt.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 27, 5, 10, 0));
    }
}
