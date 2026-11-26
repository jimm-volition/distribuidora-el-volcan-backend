package gaselvolcan.auth.controller;

import gaselvolcan.auth.dto.LoginRequest;
import gaselvolcan.auth.dto.RegistroRequest;
import gaselvolcan.auth.dto.SesionResponse;
import gaselvolcan.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Login, registro de clientes y sesion con JWT")
public class AuthController {
    private final AuthService servicio;

    public AuthController(AuthService servicio) { this.servicio = servicio; }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion",
        description = "Valida correo y contrasena (BCrypt) y devuelve un JWT firmado con el rol del usuario.")
    @ApiResponse(responseCode = "200", description = "Login correcto, incluye el token")
    @ApiResponse(responseCode = "400", description = "Datos con formato invalido")
    @ApiResponse(responseCode = "401", description = "Correo o contrasena incorrectos, o usuario desactivado")
    public SesionResponse login(@Valid @RequestBody LoginRequest req) {
        return servicio.login(req.correo(), req.password());
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar cliente",
        description = "Crea un usuario con rol CLIENTE y devuelve su token (queda con la sesion iniciada).")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "400", description = "Datos con formato invalido")
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    public ResponseEntity<SesionResponse> registro(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarCliente(req));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Usuario de la sesion",
        description = "Devuelve los datos del dueno del token. React lo usa para confirmar la sesion al recargar la pagina.")
    @ApiResponse(responseCode = "200", description = "Token valido")
    @ApiResponse(responseCode = "401", description = "Sin token, token vencido o alterado, o usuario desactivado")
    public SesionResponse me(Authentication usuario) {
        return servicio.sesionActual(usuario.getName());
    }
}
