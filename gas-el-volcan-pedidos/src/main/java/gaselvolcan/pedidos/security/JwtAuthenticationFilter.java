package gaselvolcan.pedidos.security;

import gaselvolcan.pedidos.exception.ApiError;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Igual que el filtro del ejemplo, pero con respuestas de error JSON. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final JsonMapper json;

    public JwtAuthenticationFilter(JwtService jwt, JsonMapper json) {
        this.jwt = jwt;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        try {
            Claims claims = jwt.validar(header.substring(7));
            String correo = claims.getSubject();
            String rol = claims.get("rol", String.class);
            if (correo == null || correo.isBlank() || rol == null || rol.isBlank()) {
                throw new JwtException("JWT sin correo o rol");
            }
            rol = rol.toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "");
            if (!Set.of("ADMIN", "VENDEDOR", "CLIENTE").contains(rol)) {
                throw new JwtException("Rol desconocido: " + rol);
            }
            var auth = new UsernamePasswordAuthenticationToken(correo, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            json.writeValue(res.getWriter(), new ApiError(LocalDateTime.now(), 401,
                "Unauthorized", "Token JWT inválido, vencido o emitido por otro servicio",
                req.getRequestURI(), Map.of()));
        }
    }
}
