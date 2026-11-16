package edu.dosw.styleradar.notificaciones.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorreoPendienteTest {

    @Test
    @DisplayName("GJ-119 - Un correo nuevo queda PENDIENTE, sin intentos de recuperacion y con su error de origen")
    void correoNuevoQuedaPendiente() {
        CorreoPendiente correo = CorreoPendiente.nuevo(7L, "ana@styleradar.co", "Asunto", "Cuerpo", 3, "smtp caido");

        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(correo.getIntentos()).isZero();
        assertThat(correo.getMaxIntentos()).isEqualTo(3);
        assertThat(correo.getUltimoError()).isEqualTo("smtp caido");
        assertThat(correo.getFechaCreacion()).isNotNull();
        assertThat(correo.puedeReintentarse()).isTrue();
    }

    @Test
    @DisplayName("GJ-119 - Cada fallo cuenta un intento y al llegar al maximo el correo queda FALLIDO")
    void alAgotarLosIntentosQuedaFallido() {
        CorreoPendiente correo = CorreoPendiente.nuevo(7L, "ana@styleradar.co", "Asunto", "Cuerpo", 3, null);

        correo.registrarFallo("uno");
        correo.registrarFallo("dos");
        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.PENDIENTE);
        assertThat(correo.getIntentos()).isEqualTo(2);
        assertThat(correo.puedeReintentarse()).isTrue();

        correo.registrarFallo("tres");
        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.FALLIDO);
        assertThat(correo.getIntentos()).isEqualTo(3);
        assertThat(correo.getUltimoError()).isEqualTo("tres");
        assertThat(correo.puedeReintentarse()).isFalse();
    }

    @Test
    @DisplayName("GJ-119 - Un correo enviado ya no se reintenta")
    void correoEnviadoNoSeReintenta() {
        CorreoPendiente correo = CorreoPendiente.nuevo(7L, "ana@styleradar.co", "Asunto", "Cuerpo", 3, null);

        correo.marcarEnviado();

        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.ENVIADO);
        assertThat(correo.puedeReintentarse()).isFalse();
    }

    @Test
    @DisplayName("GJ-119 - Un error muy largo se recorta y un maximo no positivo se lleva a 1")
    void errorLargoSeRecortaYMaximoMinimoUno() {
        CorreoPendiente correo = CorreoPendiente.nuevo(7L, "a@b.co", "A", "C", 0, "x".repeat(5000));

        assertThat(correo.getUltimoError()).hasSize(1000);
        assertThat(correo.getMaxIntentos()).isEqualTo(1);
        correo.registrarFallo(null);
        assertThat(correo.getEstado()).isEqualTo(EstadoCorreo.FALLIDO);
        assertThat(correo.getUltimoError()).isNull();
    }
}
