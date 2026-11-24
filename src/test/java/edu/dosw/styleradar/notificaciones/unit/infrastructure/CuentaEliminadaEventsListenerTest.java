package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.dosw.styleradar.notificaciones.application.dto.event.CuentaEliminadaEventPayload;
import edu.dosw.styleradar.notificaciones.domain.port.in.EliminarDatosUsuarioUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp.CuentaEliminadaEventsListener;
import edu.dosw.styleradar.notificaciones.infrastructure.config.FlexibleLocalDateTimeDeserializer;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@DisplayName("Consumidor de cuenta eliminada (ms-Users)")
class CuentaEliminadaEventsListenerTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .addModule(new SimpleModule().addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer()))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private EliminarDatosUsuarioUseCase useCase;
    private CuentaEliminadaEventsListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(EliminarDatosUsuarioUseCase.class);
        listener = new CuentaEliminadaEventsListener(useCase);
    }

    @Test
    @DisplayName("Lee el formato de ms-Users (userId y deletedAt en UTC)")
    void leeFormatoDeUsers() throws Exception {
        CuentaEliminadaEventPayload payload = mapper.readValue(
                "{\"userId\":42,\"deletedAt\":\"2026-11-24T10:15:30Z\"}", CuentaEliminadaEventPayload.class);

        assertThat(payload.usuarioId()).isEqualTo(42L);
        assertThat(payload.fechaEliminacion()).isEqualTo(LocalDateTime.of(2026, 11, 24, 10, 15, 30));
    }

    @Test
    @DisplayName("Tambien acepta usuarioId y el sobre con data")
    void leeFormatoAlterno() throws Exception {
        assertThat(mapper.readValue("{\"usuarioId\":7}", CuentaEliminadaEventPayload.class).usuarioId())
                .isEqualTo(7L);
        assertThat(mapper.readValue("{\"eventId\":\"e\",\"data\":{\"userId\":9}}", CuentaEliminadaEventPayload.class)
                .usuarioId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Un evento valido borra los datos del usuario")
    void eventoValido() {
        listener.onCuentaEliminada(new CuentaEliminadaEventPayload("e-1", 42L, LocalDateTime.now()));

        verify(useCase).eliminarDatosDeUsuario(42L);
    }

    @Test
    @DisplayName("Un evento sin usuarioId va a la cola de errores sin reintentos")
    void eventoInvalido() {
        assertThatThrownBy(() -> listener.onCuentaEliminada(new CuentaEliminadaEventPayload("e-2", null, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.onCuentaEliminada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(useCase);
    }
}
