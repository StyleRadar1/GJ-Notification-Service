package edu.dosw.styleradar.notificaciones.domain.port.in;

public interface ReenviarCorreosPendientesUseCase {

    /** Reintenta los correos pendientes y devuelve cuantos se enviaron. */
    int reenviarPendientes();
}
