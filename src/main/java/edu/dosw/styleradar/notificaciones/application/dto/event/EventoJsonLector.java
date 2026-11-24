package edu.dosw.styleradar.notificaciones.application.dto.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Lector tolerante de eventos externos: acepta el formato plano y el sobre con campo {@code data}
 * (el que publican ms-Inventory y ms-Payment). Busca cada campo primero en {@code data} y luego en la raiz,
 * probando varios nombres posibles.
 */
final class EventoJsonLector {

    private final JsonNode raiz;
    private final JsonNode data;

    EventoJsonLector(JsonNode raiz) {
        this.raiz = raiz == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : raiz;
        JsonNode sobre = this.raiz.path("data");
        this.data = sobre.isObject() ? sobre : this.raiz;
    }

    private JsonNode buscar(String... nombres) {
        for (String nombre : nombres) {
            JsonNode valor = data.path(nombre);
            if (esUtil(valor)) {
                return valor;
            }
            valor = raiz.path(nombre);
            if (esUtil(valor)) {
                return valor;
            }
        }
        return null;
    }

    private static boolean esUtil(JsonNode valor) {
        return valor != null && !valor.isMissingNode() && !valor.isNull();
    }

    String texto(String... nombres) {
        JsonNode valor = buscar(nombres);
        if (valor == null || valor.isContainerNode()) {
            return null;
        }
        String texto = valor.asText();
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    Long largo(String... nombres) {
        String texto = texto(nombres);
        if (texto == null) {
            return null;
        }
        try {
            return Long.valueOf(texto);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    Integer entero(String... nombres) {
        Long valor = largo(nombres);
        return valor == null ? null : Math.toIntExact(valor);
    }

    BigDecimal decimal(String... nombres) {
        String texto = texto(nombres);
        if (texto == null) {
            return null;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Acepta ISO sin zona, ISO con {@code Z} o desplazamiento, y epoch en segundos. */
    LocalDateTime fecha(String... nombres) {
        JsonNode valor = buscar(nombres);
        if (valor == null) {
            return null;
        }
        if (valor.isNumber()) {
            double segundos = valor.asDouble();
            return LocalDateTime.ofEpochSecond((long) segundos, 0, ZoneOffset.UTC);
        }
        String texto = valor.asText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        try {
            return LocalDateTime.parse(limpio);
        } catch (DateTimeParseException ignorada) {
            try {
                return OffsetDateTime.parse(limpio).toLocalDateTime();
            } catch (DateTimeParseException ignorada2) {
                try {
                    return LocalDateTime.ofInstant(Instant.parse(limpio), ZoneOffset.UTC);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
        }
    }
}
