package edu.dosw.styleradar.notificaciones.unit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.dosw.styleradar.notificaciones.domain.exception.AccesoDenegadoException;
import edu.dosw.styleradar.notificaciones.infrastructure.security.AuthenticatedUserPrincipal;
import edu.dosw.styleradar.notificaciones.infrastructure.security.SecurityContextUserExtractor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@DisplayName("GJ-93 - El usuario autenticado sale siempre del contexto de seguridad, nunca de un valor por defecto")
class SecurityContextUserExtractorTest {

    private final SecurityContextUserExtractor extractor = new SecurityContextUserExtractor();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tomaElIdDelPrincipalDelToken() {
        autenticar(new AuthenticatedUserPrincipal(501L, "mateo@styleradar.co", "COMPRADOR"));

        assertThat(extractor.obtenerUsuarioAutenticadoId()).isEqualTo(501L);
    }

    @Test
    void tomaElIdNumericoDeUnUserDetails() {
        autenticar(new User("777", "x", List.of()));

        assertThat(extractor.obtenerUsuarioAutenticadoId()).isEqualTo(777L);
    }

    @Test
    void unNombreNoNumericoNoSeConvierteEnOtroUsuario() {
        autenticar(new User("mateo", "x", List.of()));

        assertThatThrownBy(extractor::obtenerUsuarioAutenticadoId).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void sinAutenticacionSeNiegaElAcceso() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(extractor::obtenerUsuarioAutenticadoId).isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void validarAccesoUsuarioPermiteAliasMeYIdCoincidente() {
        autenticar(new AuthenticatedUserPrincipal(101L, "ana@styleradar.co", "COMPRADOR"));

        assertThat(extractor.validarAccesoUsuario("me")).isEqualTo(101L);
        assertThat(extractor.validarAccesoUsuario("101")).isEqualTo(101L);
    }

    @Test
    void validarAccesoUsuarioRechazaIdDiferenteOInvalido() {
        autenticar(new AuthenticatedUserPrincipal(101L, "ana@styleradar.co", "COMPRADOR"));

        assertThatThrownBy(() -> extractor.validarAccesoUsuario("202"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No tienes autorizacion para acceder o modificar los recursos del usuario 202");

        assertThatThrownBy(() -> extractor.validarAccesoUsuario("abc"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("Identificador de usuario invalido");
    }

    private void autenticar(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
