package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerUiRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUiRedirigeALaPaginaPrincipal() throws Exception {
        mockMvc.perform(get("/swagger-ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void paginaPrincipalDeSwaggerUiRespondeSinRedireccion() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void contratoOpenApiEstaDisponibleSinToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void todosLosEndpointsExpuestosEnOpenApiComienzanConApiV1() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        com.fasterxml.jackson.databind.JsonNode paths = root.get("paths");
        org.assertj.core.api.Assertions.assertThat(paths).isNotNull();

        java.util.Iterator<String> fieldNames = paths.fieldNames();
        org.assertj.core.api.Assertions.assertThat(fieldNames.hasNext()).isTrue();

        while (fieldNames.hasNext()) {
            String path = fieldNames.next();
            org.assertj.core.api.Assertions.assertThat(path)
                    .as("El path de Swagger/OpenAPI debe comenzar con /api/v1: " + path)
                    .startsWith("/api/v1/");
            org.assertj.core.api.Assertions.assertThat(path)
                    .as("El path no debe contener /me: " + path)
                    .doesNotContain("/me");
        }
    }
}
