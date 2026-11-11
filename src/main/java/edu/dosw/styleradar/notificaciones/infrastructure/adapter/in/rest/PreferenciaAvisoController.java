package edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.notificaciones.application.dto.request.PreferenciaAvisoRequest;
import edu.dosw.styleradar.notificaciones.application.dto.response.PreferenciaAvisoResponse;
import edu.dosw.styleradar.notificaciones.domain.model.PreferenciaAviso;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.in.PreferenciasAvisosUseCase;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.rest.doc.PreferenciaAvisoApi;
import edu.dosw.styleradar.notificaciones.infrastructure.security.SecurityContextUserExtractor;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios/me/preferencias-avisos")
@RequiredArgsConstructor
public class PreferenciaAvisoController implements PreferenciaAvisoApi {

    private final PreferenciasAvisosUseCase preferenciasAvisosUseCase;
    private final SecurityContextUserExtractor userExtractor;

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<PreferenciaAvisoResponse>> listarMisPreferencias() {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        return ResponseEntity.ok(preferenciasAvisosUseCase.obtenerPreferencias(usuarioId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{tipo}")
    public ResponseEntity<PreferenciaAvisoResponse> guardarPreferencia(
            @PathVariable("tipo") TipoNotificacion tipo,
            @Valid @RequestBody PreferenciaAvisoRequest request
    ) {
        Long usuarioId = userExtractor.obtenerUsuarioAutenticadoId();
        PreferenciaAviso guardada = preferenciasAvisosUseCase.guardarPreferencia(usuarioId, tipo, request.canal());
        return ResponseEntity.ok(toResponse(guardada));
    }

    private PreferenciaAvisoResponse toResponse(PreferenciaAviso preferencia) {
        return new PreferenciaAvisoResponse(preferencia.getTipo(), preferencia.getCanal());
    }
}
