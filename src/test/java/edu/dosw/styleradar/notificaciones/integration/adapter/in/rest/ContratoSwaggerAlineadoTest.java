package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Mantiene alineados el Swagger, el README y lo que el servicio responde de verdad: lo que se documenta debe
 * ser lo que existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Swagger, README y respuestas reales alineados")
class ContratoSwaggerAlineadoTest {

    private static final Pattern FILA_README = Pattern.compile(
            "\\|\\s*`(GET|POST|PUT|PATCH|DELETE)`\\s*\\|\\s*`(/api/v1/users/[^`]+)`");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private JsonNode contrato;

    @BeforeEach
    void cargarContrato() throws Exception {
        String cuerpo = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        contrato = objectMapper.readTree(cuerpo);
    }

    @Test
    @DisplayName("Las rutas del README son exactamente las del Swagger")
    void rutasDelReadmeIgualesALasDelSwagger() throws Exception {
        Set<String> swagger = new HashSet<>();
        contrato.get("paths").fields().forEachRemaining(ruta -> ruta.getValue().fieldNames()
                .forEachRemaining(metodo -> swagger.add(metodo.toUpperCase() + " " + ruta.getKey())));

        String readme = Files.readString(Path.of("docs/readme/03-endpoints-y-swagger.md"), StandardCharsets.UTF_8);
        Set<String> documentadas = new HashSet<>();
        Matcher fila = FILA_README.matcher(readme);
        while (fila.find()) {
            documentadas.add(fila.group(1) + " " + fila.group(2));
        }

        assertThat(documentadas).as("rutas del README").isEqualTo(swagger);
    }

    @Test
    @DisplayName("Los codigos HTTP de cada ruta del README son los del Swagger")
    void codigosDelReadmeIgualesALosDelSwagger() throws Exception {
        String readme = Files.readString(Path.of("docs/readme/03-endpoints-y-swagger.md"), StandardCharsets.UTF_8);
        Pattern fila = Pattern.compile("\\|\\s*`(GET|POST|PUT|PATCH|DELETE)`\\s*\\|\\s*`(/api/v1/users/[^`]+)`[^\\r\\n]*\\|\\s*((?:`\\d{3}`(?:,\\s*)?)+)\\s*\\|");
        Matcher m = fila.matcher(readme);
        int comprobadas = 0;
        while (m.find()) {
            Set<String> enReadme = new java.util.TreeSet<>();
            Matcher codigo = Pattern.compile("\\d{3}").matcher(m.group(3));
            while (codigo.find()) {
                enReadme.add(codigo.group());
            }
            Set<String> enSwagger = new java.util.TreeSet<>();
            contrato.at("/paths/" + m.group(2).replace("/", "~1") + "/" + m.group(1).toLowerCase() + "/responses")
                    .fieldNames().forEachRemaining(enSwagger::add);
            assertThat(enReadme).as(m.group(1) + " " + m.group(2)).isEqualTo(enSwagger);
            comprobadas++;
        }
        assertThat(comprobadas).isEqualTo(16);
    }

    @Test
    @DisplayName("La lista de notificaciones muestra un solo filtro de lectura: read (mas page y size)")
    void unSoloFiltroDeLecturaEnElSwagger() {
        JsonNode parametros = contrato.at("/paths/~1api~1v1~1users~1{id}~1notifications/get/parameters");
        Set<String> visibles = new java.util.TreeSet<>();
        parametros.forEach(p -> visibles.add(p.get("name").asText()));

        assertThat(visibles).containsExactlyInAnyOrder("id", "read", "page", "size");
    }

    @Test
    @DisplayName("El esquema del error lista los codigos estables y los ejemplos traen el codigo de su estado")
    void codigosDeErrorEnElSwagger() {
        JsonNode codigos = contrato.at("/components/schemas/ProblemDetail/properties/code/enum");
        Set<String> lista = new java.util.TreeSet<>();
        codigos.forEach(c -> lista.add(c.asText()));
        assertThat(lista).contains("VALIDATION_ERROR", "UNAUTHENTICATED", "ACCESS_DENIED", "ALERT_NOT_FOUND",
                "ALERT_ALREADY_ACTIVE", "ITEM_HAS_STOCK", "SAVED_SEARCH_NOT_AVAILABLE", "RATE_LIMITED", "CATALOG_UNAVAILABLE");

        JsonNode ejemplos = contrato.at("/paths/~1api~1v1~1users~1{id}~1alerts/post/responses");
        for (String estado : new String[] {"400", "401", "403", "409", "422", "503"}) {
            JsonNode ejemplo = ejemplos.get(estado).at("/content/application~1problem+json/examples").elements().next().get("value");
            assertThat(lista).as(estado).contains(ejemplo.get("code").asText());
        }
    }

    @Test
    @DisplayName("Todos los textos del Swagger estan en ingles: los esquemas no traen palabras en espanol")
    void swaggerEnIngles() {
        Pattern espanol = Pattern.compile("(?i)[áéíóúñ]|\\b(el|la|los|las|del|para|con|sin|una|usuario|prenda|alerta|campana|talla)\\b");
        java.util.List<String> encontrados = new java.util.ArrayList<>();
        contrato.at("/components/schemas").fields().forEachRemaining(esquema -> {
            esquema.getValue().path("properties").fields().forEachRemaining(prop -> {
                String descripcion = prop.getValue().path("description").asText("");
                if (espanol.matcher(descripcion).find()) {
                    encontrados.add(esquema.getKey() + "." + prop.getKey() + ": " + descripcion);
                }
            });
            String descripcion = esquema.getValue().path("description").asText("");
            if (espanol.matcher(descripcion).find()) {
                encontrados.add(esquema.getKey() + ": " + descripcion);
            }
        });
        assertThat(encontrados).isEmpty();
    }

    @Test
    @DisplayName("Todas las operaciones documentan 401, 403 y 429")
    void todasLasOperacionesDocumentanLosErroresComunes() {
        contrato.get("paths").fields().forEachRemaining(ruta -> ruta.getValue().fields().forEachRemaining(op -> {
            JsonNode respuestas = op.getValue().get("responses");
            for (String codigo : new String[] {"401", "403", "429"}) {
                assertThat(respuestas.has(codigo)).as(op.getKey().toUpperCase() + " " + ruta.getKey() + " " + codigo).isTrue();
            }
        }));
    }

    @Test
    @DisplayName("El esquema del error trae los campos reales: mensaje, timestamp y errors")
    void esquemaDelErrorConCamposReales() {
        JsonNode propiedades = contrato.at("/components/schemas/ProblemDetail/properties");
        assertThat(propiedades.has("mensaje")).isTrue();
        assertThat(propiedades.has("timestamp")).isTrue();
        assertThat(propiedades.has("errors")).isTrue();
    }

    @Test
    @DisplayName("Los ejemplos de notificaciones solo usan campos que existen en el esquema")
    void ejemplosDeNotificacionesCoincidenConElEsquema() throws Exception {
        Set<String> campos = nombres(contrato.at("/components/schemas/NotificacionResponse/properties"));
        JsonNode ejemplos = contrato.at("/paths/~1api~1v1~1users~1{id}~1notifications/get/responses/200/content/application~1json/examples");

        JsonNode arreglo = ejemplos.get("ArrayDirectoCompatibilidad").get("value");
        assertThat(nombres(arreglo.get(0))).isSubsetOf(campos);

        JsonNode pagina = ejemplos.get("EstructuraPaginada").get("value");
        assertThat(nombres(pagina)).containsExactlyInAnyOrder(
                "content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious");
        assertThat(nombres(pagina.get("content").get(0))).isSubsetOf(campos);
    }

    @Test
    @DisplayName("Los ejemplos de error tienen el mismo formato que las respuestas reales")
    void ejemplosDeErrorConElFormatoReal() throws Exception {
        JsonNode ejemplos = contrato.at("/paths/~1api~1v1~1users~1{id}~1alerts/post/responses");
        for (String codigo : new String[] {"400", "401", "403", "409", "422", "503"}) {
            JsonNode ejemplo = ejemplos.get(codigo).at("/content/application~1problem+json/examples")
                    .elements().next().get("value");
            assertThat(ejemplo.get("type").asText()).as(codigo).isEqualTo("about:blank");
            assertThat(ejemplo.get("status").asInt()).as(codigo).isEqualTo(Integer.parseInt(codigo));
            assertThat(ejemplo.has("mensaje")).as(codigo).isTrue();
            assertThat(ejemplo.has("timestamp")).as(codigo).isTrue();
        }
    }

    @Test
    @DisplayName("Un 400 de validacion trae el mensaje por campo en errors, como dice el formato comun")
    void validacionConErroresPorCampo() throws Exception {
        String token = "Bearer " + jwtTokenService.generateToken(9_701L, "contrato@styleradar.co", "COMPRADOR");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/users/9701/alerts").header("Authorization", token)
                        .contentType("application/json").content("{\"canal\":\"IN_APP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.tipoAlerta").value("El tipo de alerta es obligatorio"))
                .andExpect(jsonPath("$.mensaje").value("El tipo de alerta es obligatorio"));

        mockMvc.perform(get("/api/v1/users/9701/notifications").param("size", "500").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").value("size no puede superar 100"));

        mockMvc.perform(get("/api/v1/users/9701/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private static Set<String> nombres(JsonNode objeto) {
        Set<String> nombres = new HashSet<>();
        Iterator<String> it = objeto.fieldNames();
        while (it.hasNext()) {
            nombres.add(it.next());
        }
        return nombres;
    }
}
