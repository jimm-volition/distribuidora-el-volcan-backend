package gaselvolcan.auth.service;

import gaselvolcan.auth.dto.RegistroRequest;
import gaselvolcan.auth.dto.SesionResponse;
import gaselvolcan.auth.exception.CorreoDuplicadoException;
import gaselvolcan.auth.exception.CredencialesInvalidasException;
import gaselvolcan.auth.model.Rol;
import gaselvolcan.auth.model.Usuario;
import gaselvolcan.auth.repository.UsuarioRepository;
import gaselvolcan.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    // Mismo mensaje para correo inexistente y clave mala: no revela que correos existen.
    static final String CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public SesionResponse login(String correo, String password) {
        Usuario u = usuarios.findByCorreoIgnoreCase(normalizar(correo))
            .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));
        if (!u.isActivo() || !encoder.matches(password, u.getClaveHash())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }
        return conToken(u);
    }

    @Transactional
    public SesionResponse registrarCliente(RegistroRequest req) {
        String correo = normalizar(req.correo());
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("Ese correo ya esta registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setClaveHash(encoder.encode(req.password()));
        u.setRol(Rol.CLIENTE);
        u.setTelefono(normalizarTelefono(req.telefono()));
        u.setActivo(true);
        return conToken(usuarios.saveAndFlush(u));
    }

    /** Datos frescos del dueno del token. Si lo desactivaron despues de emitirlo, la sesion ya no vale. */
    @Transactional(readOnly = true)
    public SesionResponse sesionActual(String correo) {
        Usuario u = usuarios.findByCorreoIgnoreCase(correo)
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new CredencialesInvalidasException("La sesion ya no es valida"));
        return new SesionResponse(null, u.getCorreo(), u.getRol(), u.getNombre());
    }

    /**
     * Renueva un token que todavia es valido (el filtro ya verifico firma, emisor y vencimiento).
     * Vuelve a leer al usuario: si lo desactivaron no renueva, y si le cambiaron el rol
     * el token nuevo trae el rol actualizado. Conserva el limite de la sesion original.
     */
    @Transactional(readOnly = true)
    public SesionResponse renovar(String correo, Claims actuales) {
        Usuario u = usuarios.findByCorreoIgnoreCase(correo)
            .filter(Usuario::isActivo)
            .orElseThrow(() -> new CredencialesInvalidasException("La sesion ya no es valida"));
        Date sesionHasta = jwt.sesionHasta(actuales);
        if (!sesionHasta.after(new Date())) {
            throw new CredencialesInvalidasException("La sesion alcanzo su duracion maxima. Vuelve a ingresar");
        }
        return new SesionResponse(jwt.generar(u, sesionHasta), u.getCorreo(), u.getRol(), u.getNombre());
    }

    private SesionResponse conToken(Usuario u) {
        return new SesionResponse(jwt.generar(u), u.getCorreo(), u.getRol(), u.getNombre());
    }

    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    /** Guarda el celular como +569XXXXXXXX, venga como venga escrito. */
    private static String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        String digitos = telefono.replaceAll("\\D", "");
        return "+56" + digitos.substring(digitos.length() - 9);
    }
}
