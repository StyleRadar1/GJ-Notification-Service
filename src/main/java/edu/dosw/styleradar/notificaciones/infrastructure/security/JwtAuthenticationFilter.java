package edu.dosw.styleradar.notificaciones.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Parametro de consulta (RFC 6750) con el que un EventSource del navegador puede enviar el token. */
    static final String PARAMETRO_TOKEN = "access_token";

    private static final List<String> RUTAS_STREAM = List.of("/notificaciones/stream", "/notifications/stream");

    private final JwtTokenService jwtTokenService;

    /**
     * El EventSource nativo del navegador no puede enviar la cabecera Authorization. Con esta opcion el stream
     * SSE (y solo el stream) tambien acepta el token en {@code ?access_token=}. La URL queda en los registros
     * de acceso del servidor, asi que conviene apagarla (SSE_QUERY_TOKEN_ENABLED=false) si el front usa un
     * cliente que si envia la cabecera, como fetch-event-source.
     */
    @Value("${application.security.sse-query-token.enabled:true}")
    private boolean tokenPorConsultaEnStream = true;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extraerToken(request);

        if (token != null) {
            if (jwtTokenService.isTokenValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long usuarioId = jwtTokenService.extractUsuarioId(token);
                String email = jwtTokenService.extractEmail(token);
                String role = jwtTokenService.extractRole(token);

                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(usuarioId, email, role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** La cabecera Authorization manda; solo si falta, el stream acepta el token en la consulta. */
    private String extraerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (tokenPorConsultaEnStream && "GET".equalsIgnoreCase(request.getMethod()) && esStream(request.getRequestURI())) {
            String token = request.getParameter(PARAMETRO_TOKEN);
            return token != null && !token.isBlank() ? token : null;
        }
        return null;
    }

    private boolean esStream(String uri) {
        return uri != null && RUTAS_STREAM.stream().anyMatch(uri::endsWith);
    }
}
