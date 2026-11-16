package edu.dosw.styleradar.notificaciones.application.usecase;

import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCorreo;
import edu.dosw.styleradar.notificaciones.domain.port.in.ReenviarCorreosPendientesUseCase;
import edu.dosw.styleradar.notificaciones.domain.port.out.CorreoPendienteRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EnvioCorreoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReenviarCorreosPendientesUseCaseImpl implements ReenviarCorreosPendientesUseCase {

    /** Cuantos correos se reintentan en cada pasada, para no atascar el hilo si hay muchos. */
    public static final int LOTE_MAXIMO = 50;

    private final CorreoPendienteRepositoryPort correoPendienteRepositoryPort;
    private final EnvioCorreoPort envioCorreoPort;

    @Override
    public int reenviarPendientes() {
        if (!envioCorreoPort.estaDisponible()) {
            log.debug("Sin servidor de correo configurado: no se reintentan los correos pendientes");
            return 0;
        }
        int enviados = 0;
        for (CorreoPendiente correo : correoPendienteRepositoryPort.listarPendientes(LOTE_MAXIMO)) {
            if (!correo.puedeReintentarse()) {
                continue;
            }
            try {
                envioCorreoPort.enviarSincrono(correo.getDestinatario(), correo.getAsunto(), correo.getCuerpo());
                correo.marcarEnviado();
                enviados++;
                log.info("Correo pendiente {} enviado", correo.getId());
            } catch (RuntimeException ex) {
                correo.registrarFallo(ex.getMessage());
                if (correo.getEstado() == EstadoCorreo.FALLIDO) {
                    log.error("Correo pendiente {} marcado como FALLIDO tras {} intentos: {}",
                            correo.getId(), correo.getIntentos(), ex.getMessage());
                } else {
                    log.warn("Fallo el reintento {} de {} del correo pendiente {}: {}",
                            correo.getIntentos(), correo.getMaxIntentos(), correo.getId(), ex.getMessage());
                }
            }
            correoPendienteRepositoryPort.guardar(correo);
        }
        return enviados;
    }
}
