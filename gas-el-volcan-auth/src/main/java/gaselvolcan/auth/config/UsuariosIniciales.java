package gaselvolcan.auth.config;

import gaselvolcan.auth.model.Rol;
import gaselvolcan.auth.model.Usuario;
import gaselvolcan.auth.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Semilla minima: un usuario por rol, para poder probar el sistema.
 * Las contrasenas vienen de variables de entorno (SEED_*) y se guardan con BCrypt.
 * Solo crea el usuario si ese correo todavia no existe.
 */
@Component
public class UsuariosIniciales implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String claveAdmin;
    private final String claveVendedor;
    private final String claveCliente;

    public UsuariosIniciales(UsuarioRepository usuarios, PasswordEncoder encoder,
                             @Value("${SEED_ADMIN_PASSWORD:}") String claveAdmin,
                             @Value("${SEED_VENDEDOR_PASSWORD:}") String claveVendedor,
                             @Value("${SEED_CLIENTE_PASSWORD:}") String claveCliente) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.claveAdmin = claveAdmin;
        this.claveVendedor = claveVendedor;
        this.claveCliente = claveCliente;
    }

    @Override
    public void run(String... args) {
        crear("Administradora", "admin@volcan.cl", claveAdmin, Rol.ADMIN, null);
        crear("Operadora", "vendedor@volcan.cl", claveVendedor, Rol.VENDEDOR, null);
        crear("Cliente Demo", "cliente@volcan.cl", claveCliente, Rol.CLIENTE, "+56911111111");
    }

    private void crear(String nombre, String correo, String clave, Rol rol, String telefono) {
        if (clave == null || clave.isBlank() || usuarios.existsByCorreoIgnoreCase(correo)) return;
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setClaveHash(encoder.encode(clave));
        u.setRol(rol);
        u.setTelefono(telefono);
        u.setActivo(true);
        usuarios.save(u);
    }
}
