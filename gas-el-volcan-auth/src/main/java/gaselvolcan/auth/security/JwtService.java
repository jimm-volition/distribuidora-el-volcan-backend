package gaselvolcan.auth.security;

import gaselvolcan.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite y valida el JWT. Auth es el UNICO servicio que firma tokens;
 * Pedidos solo los valida con la misma clave y el mismo emisor.
 *
 * Contrato del token (lo que lee Pedidos):
 *   iss = gas-el-volcan-auth, sub = correo, rol = ADMIN | VENDEDOR | CLIENTE, exp = vencimiento.
 * Ademas lleva "sesionHasta": hasta cuando se puede renovar (POST /api/v1/auth/refresh).
 */
@Service
public class JwtService {
    private final SecretKey clave;
    private final String emisor;
    private final long duracionMs;
    private final long maxSesionMs;

    /** Constructor para pruebas: sesion maxima de 8 horas. */
    public JwtService(String secretBase64, String issuer, long duracionMs) {
        this(secretBase64, issuer, duracionMs, 8 * 60 * 60 * 1000L);
    }

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expiration-ms}") long duracionMs,
                      @Value("${app.jwt.max-session-ms}") long maxSesionMs) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("Configura JWT_SECRET (Base64, igual que Pedidos)");
        }
        try {
            byte[] bytes = Decoders.BASE64.decode(secretBase64);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET debe representar al menos 32 bytes");
            }
            this.clave = Keys.hmacShaKeyFor(bytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET no es un Base64 valido", ex);
        }
        this.emisor = Objects.requireNonNull(issuer, "app.jwt.issuer");
        this.duracionMs = duracionMs;
        this.maxSesionMs = maxSesionMs;
    }

    /** Token de un login nuevo: la sesion puede renovarse hasta maxSesionMs desde ahora. */
    public String generar(Usuario usuario) {
        return generar(usuario, new Date(System.currentTimeMillis() + maxSesionMs));
    }

    /**
     * Token con limite de sesion fijo. Al renovar se conserva el "sesionHasta" del login original,
     * asi una sesion no se puede alargar para siempre. El token nunca vence despues de ese limite.
     */
    public String generar(Usuario usuario, Date sesionHasta) {
        Date ahora = new Date();
        long vence = Math.min(ahora.getTime() + duracionMs, sesionHasta.getTime());
        return Jwts.builder()
            .issuer(emisor)
            .subject(usuario.getCorreo())
            .claim("rol", usuario.getRol().name())
            .claim("nombre", usuario.getNombre())
            .claim("sesionHasta", sesionHasta.getTime() / 1000)
            .issuedAt(ahora)
            .expiration(new Date(vence))
            .signWith(clave)
            .compact();
    }

    /** Limite de la sesion a la que pertenece el token (claim "sesionHasta", en segundos). */
    public Date sesionHasta(Claims claims) {
        if (claims.get("sesionHasta") instanceof Number segundos) {
            return new Date(segundos.longValue() * 1000);
        }
        // Token sin el claim: se cuenta desde su emision.
        return new Date(claims.getIssuedAt().getTime() + maxSesionMs);
    }

    /** Verifica firma, emisor y vencimiento. Lanza JwtException si algo no cuadra. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).requireIssuer(emisor)
            .build().parseSignedClaims(token).getPayload();
    }
}
