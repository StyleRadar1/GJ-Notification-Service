package edu.dosw.styleradar.notificaciones.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Parametro de consulta con el ticket de vida corta del stream SSE (ver {@code JwtTokenService}). */
    static final String PARAMETRO_TICKET = "ticket";

    private static final String RUTA_STREAM = "/notifications/stream";

    private final JwtTokenService jwtTokenService;

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
                List<String> roles = jwtTokenService.extractRoles(token);
                String primaryRole = jwtTokenService.extractRole(token);

                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(usuarioId, email, primaryRole, roles);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Un JWT de sesion solo se acepta en la cabecera Authorization. El stream SSE tambien acepta, y solo por
     * GET, un ticket de vida corta en {@code ?ticket=}: un JWT de sesion en la URL se rechaza y un ticket en
     * la cabecera tampoco sirve.
     */
    private String extraerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtTokenService.esTicketStream(token) ? null : token;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && esStream(request.getRequestURI())) {
            String ticket = request.getParameter(PARAMETRO_TICKET);
            if (ticket != null && !ticket.isBlank() && jwtTokenService.esTicketStream(ticket)) {
                return ticket;
            }
        }
        return null;
    }

    private boolean esStream(String uri) {
        return uri != null && uri.endsWith(RUTA_STREAM);
    }
}
