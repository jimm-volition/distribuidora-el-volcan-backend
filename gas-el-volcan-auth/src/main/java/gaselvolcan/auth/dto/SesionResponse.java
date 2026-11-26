package gaselvolcan.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import gaselvolcan.auth.model.Rol;

/**
 * Respuesta de login, registro y /me. Es el contrato que consume React:
 * { token, correo, rol, nombre }. En /me el token no se repite (el cliente ya lo tiene).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SesionResponse(String token, String correo, Rol rol, String nombre) {}
