package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void metricsExigeAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STYLERADAR_ADMIN")
    void metricsPermiteStyleradarAdmin() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void metricsNiegaBuyer() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
    }

    @Test
    void infoExigeAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }
}
