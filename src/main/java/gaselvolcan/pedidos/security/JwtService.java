package gaselvolcan.pedidos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Valida JWT emitidos por Auth: misma clave Base64, firma HMAC y emisor. */
@Service
public class JwtService {
    private final SecretKey clave;
    private final String emisor;

    public JwtService(@Value("${app.jwt.secret}") String secretBase64,
                      @Value("${app.jwt.issuer}") String issuer) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("Configura JWT_SECRET (Base64, igual que Auth)");
        }
        try {
            byte[] bytes = Decoders.BASE64.decode(secretBase64);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET debe representar al menos 32 bytes");
            }
            this.clave = Keys.hmacShaKeyFor(bytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET no es un Base64 válido", ex);
        }
        this.emisor = Objects.requireNonNull(issuer, "app.jwt.issuer");
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).requireIssuer(emisor)
            .build().parseSignedClaims(token).getPayload();
    }
}
