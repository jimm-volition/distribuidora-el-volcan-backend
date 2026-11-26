package gaselvolcan.auth.security;

import gaselvolcan.auth.model.Rol;
import gaselvolcan.auth.model.Usuario;
import io.jsonwebtoken.JwtException;
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
    private final JwtService jwt = new JwtService(claveBase64, "gas-el-volcan-auth", 60_000);

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setCorreo("vendedor@volcan.cl");
        u.setNombre("Operadora");
        u.setRol(Rol.VENDEDOR);
        return u;
    }

    @Test void emiteTokenConElContratoQueLeePedidos() {
        var claims = jwt.validar(jwt.generar(usuario()));
        assertEquals("gas-el-volcan-auth", claims.getIssuer());
        assertEquals("vendedor@volcan.cl", claims.getSubject());
        assertEquals("VENDEDOR", claims.get("rol", String.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test void rechazaTokenAlterado() {
        String token = jwt.generar(usuario());
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        assertThrows(JwtException.class, () -> jwt.validar(alterado));
    }

    @Test void rechazaTokenVencido() {
        String vencido = Jwts.builder().issuer("gas-el-volcan-auth").subject("x@volcan.cl").claim("rol", "ADMIN")
            .expiration(new Date(System.currentTimeMillis() - 1_000))
            .signWith(Keys.hmacShaKeyFor(bytes)).compact();
        assertThrows(JwtException.class, () -> jwt.validar(vencido));
    }

    @Test void rechazaTokenFirmadoConOtraClave() {
        JwtService otro = new JwtService(Base64.getEncoder().encodeToString(
            "otra-clave-distinta-de-32-bytes!!".getBytes(StandardCharsets.UTF_8)), "gas-el-volcan-auth", 60_000);
        assertThrows(JwtException.class, () -> jwt.validar(otro.generar(usuario())));
    }

    @Test void exigeUnSecretoDeAlMenos32Bytes() {
        String corto = Base64.getEncoder().encodeToString("corto".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, () -> new JwtService(corto, "gas-el-volcan-auth", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("", "gas-el-volcan-auth", 60_000));
    }
}
