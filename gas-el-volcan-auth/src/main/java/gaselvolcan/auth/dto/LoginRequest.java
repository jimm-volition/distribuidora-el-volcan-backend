package gaselvolcan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "es obligatorio") @Email(message = "formato de correo invalido") String correo,
    @NotBlank(message = "es obligatoria") String password
) {}
