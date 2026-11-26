package gaselvolcan.pedidos.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final byte[] bytes = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
    private final String claveBase64 = Base64.getEncoder().encodeToString(bytes);

    private String firmar(String emisor) {
        return Jwts.builder()
            .issuer(emisor).subject("cliente@ejemplo.cl")
            .claim("rol", "CLIENTE")
            .expiration(new Date(System.currentTimeMillis()+60_000))
            .signWith(Keys.hmacShaKeyFor(bytes)).compact();
    }

    @Test void aceptaEmisorYClaveDelAuth() {
        JwtService jwt = new JwtService(claveBase64, "gas-el-volcan-auth");
        var datos = jwt.validar(firmar("gas-el-volcan-auth"));
        assertEquals("cliente@ejemplo.cl", datos.getSubject());
        assertEquals("CLIENTE", datos.get("rol", String.class));
    }

    @Test void rechazaOtroEmisor() {
        JwtService jwt = new JwtService(claveBase64, "gas-el-volcan-auth");
        assertThrows(io.jsonwebtoken.JwtException.class,
            () -> jwt.validar(firmar("otro-sistema-auth")));
    }
}
