package edu.dosw.styleradar.notificaciones.domain.port.out;

import edu.dosw.styleradar.notificaciones.domain.model.CorreoPendiente;
import java.util.List;
import java.util.Optional;

public interface CorreoPendienteRepositoryPort {

    CorreoPendiente guardar(CorreoPendiente correo);

    Optional<CorreoPendiente> buscarPorId(Long id);

    /** Correos PENDIENTES con intentos por debajo de su maximo, los mas antiguos primero. */
    List<CorreoPendiente> listarPendientes(int limite);

    /** Borra los correos pendientes del usuario (cuenta eliminada) para no enviarle nada mas. */
    int eliminarPorUsuario(Long usuarioId);
}
