package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.scheduler;

import edu.dosw.styleradar.notificaciones.domain.port.in.ReenviarCorreosPendientesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los correos pendientes al arrancar el servicio (los que quedaron de antes de un reinicio) y
 * despues cada {@code application.mail.retry-delay-ms}. Un fallo aqui nunca debe tumbar el servicio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoPendienteRecuperador {

    private final ReenviarCorreosPendientesUseCase reenviarCorreosPendientesUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        recuperar("arranque");
    }

    @Scheduled(
            fixedDelayString = "${application.mail.retry-delay-ms:60000}",
            initialDelayString = "${application.mail.retry-delay-ms:60000}")
    public void periodicamente() {
        recuperar("programado");
    }

    private void recuperar(String origen) {
        try {
            int enviados = reenviarCorreosPendientesUseCase.reenviarPendientes();
            if (enviados > 0) {
                log.info("Reintento de correos pendientes ({}): {} enviados", origen, enviados);
            }
        } catch (RuntimeException ex) {
            log.error("Fallo el reintento de correos pendientes ({})", origen, ex);
        }
    }
}
