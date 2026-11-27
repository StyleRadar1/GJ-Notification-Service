package edu.dosw.styleradar.notificaciones.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Deserializa {@link LocalDateTime} aceptando marcas ISO-8601 sin zona, fechas con sufijo {@code Z} o
 * desplazamiento UTC, y marcas numéricas epoch (segundos enteros o decimales) emitidas por microservicios
 * que serializan {@code Instant} o {@code OffsetDateTime}.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            String trimmed = text.trim();
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(trimmed).toLocalDateTime();
                } catch (DateTimeParseException ex) {
                    throw ctxt.weirdStringException(
                            trimmed, LocalDateTime.class, "Formato de fecha ISO-8601 invalido");
                }
            }
        }
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
            BigDecimal decimal = p.getDecimalValue();
            long epochSeconds = decimal.longValue();
            long nanos = decimal.subtract(BigDecimal.valueOf(epochSeconds))
                    .movePointRight(9)
                    .longValue();
            Instant instant = Instant.ofEpochSecond(epochSeconds, nanos);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        return LocalDateTimeDeserializer.INSTANCE.deserialize(p, ctxt);
    }
}
