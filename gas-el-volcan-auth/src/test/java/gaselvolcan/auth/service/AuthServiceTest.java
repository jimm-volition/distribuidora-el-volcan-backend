package gaselvolcan.auth.service;

import gaselvolcan.auth.dto.RegistroRequest;
import gaselvolcan.auth.exception.CorreoDuplicadoException;
import gaselvolcan.auth.exception.CredencialesInvalidasException;
import gaselvolcan.auth.model.Rol;
import gaselvolcan.auth.model.Usuario;
import gaselvolcan.auth.repository.UsuarioRepository;
import gaselvolcan.auth.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock JwtService jwt;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService servicio;

    @BeforeEach void preparar() {
        servicio = new AuthService(usuarios, encoder, jwt);
    }

    private Usuario guardado(String clave, boolean activo) {
        Usuario u = new Usuario();
        u.setNombre("Cliente Demo");
        u.setCorreo("cliente@volcan.cl");
        u.setClaveHash(encoder.encode(clave));
        u.setRol(Rol.CLIENTE);
        u.setActivo(activo);
        return u;
    }

    @Test void loginCorrectoDevuelveTokenYRol() {
        when(usuarios.findByCorreoIgnoreCase("cliente@volcan.cl")).thenReturn(Optional.of(guardado("Cliente123", true)));
        when(jwt.generar(any())).thenReturn("token-firmado");

        var sesion = servicio.login("  Cliente@Volcan.cl ", "Cliente123");

        assertEquals("token-firmado", sesion.token());
        assertEquals(Rol.CLIENTE, sesion.rol());
        assertEquals("Cliente Demo", sesion.nombre());
    }

    @Test void loginConClaveMalaEs401() {
        when(usuarios.findByCorreoIgnoreCase("cliente@volcan.cl")).thenReturn(Optional.of(guardado("Cliente123", true)));
        assertThrows(CredencialesInvalidasException.class, () -> servicio.login("cliente@volcan.cl", "otraClave"));
        verify(jwt, never()).generar(any());
    }

    @Test void loginDeCorreoInexistenteDaElMismoMensaje() {
        when(usuarios.findByCorreoIgnoreCase(any())).thenReturn(Optional.empty());
        var e = assertThrows(CredencialesInvalidasException.class, () -> servicio.login("nadie@volcan.cl", "x"));
        assertEquals(AuthService.CREDENCIALES_INVALIDAS, e.getMessage());
    }

    @Test void usuarioDesactivadoNoPuedeIngresar() {
        when(usuarios.findByCorreoIgnoreCase("cliente@volcan.cl")).thenReturn(Optional.of(guardado("Cliente123", false)));
        assertThrows(CredencialesInvalidasException.class, () -> servicio.login("cliente@volcan.cl", "Cliente123"));
    }

    @Test void registroCreaSiempreClienteConClaveHasheada() {
        when(usuarios.existsByCorreoIgnoreCase("nuevo@volcan.cl")).thenReturn(false);
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwt.generar(any())).thenReturn("token");

        servicio.registrarCliente(new RegistroRequest(" Ana ", "Nuevo@Volcan.cl", "Clave12345", "9 8765 4321"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        Usuario u = captor.getValue();
        assertEquals(Rol.CLIENTE, u.getRol());
        assertEquals("nuevo@volcan.cl", u.getCorreo());
        assertEquals("Ana", u.getNombre());
        assertEquals("+56987654321", u.getTelefono());
        assertNotEquals("Clave12345", u.getClaveHash());
        assertTrue(encoder.matches("Clave12345", u.getClaveHash()));
    }

    @Test void registroConCorreoRepetidoEs409() {
        when(usuarios.existsByCorreoIgnoreCase("cliente@volcan.cl")).thenReturn(true);
        assertThrows(CorreoDuplicadoException.class, () ->
            servicio.registrarCliente(new RegistroRequest("Ana", "cliente@volcan.cl", "Clave12345", null)));
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test void sesionActualFallaSiElUsuarioFueDesactivado() {
        when(usuarios.findByCorreoIgnoreCase("cliente@volcan.cl")).thenReturn(Optional.of(guardado("x", false)));
        assertThrows(CredencialesInvalidasException.class, () -> servicio.sesionActual("cliente@volcan.cl"));
    }
}
