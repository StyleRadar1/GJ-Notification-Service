package edu.dosw.styleradar.notificaciones.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Deserializa {@link LocalDateTime} aceptando tanto marcas ISO-8601 sin zona como fechas con sufijo {@code Z} o
 * desplazamiento UTC, para evitar fallos en eventos emitidos por microservicios que serializan {@code Instant} o
 * {@code OffsetDateTime}.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
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
        return LocalDateTimeDeserializer.INSTANCE.deserialize(p, ctxt);
    }
}
