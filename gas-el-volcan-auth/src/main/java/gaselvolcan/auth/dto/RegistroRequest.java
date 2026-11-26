package gaselvolcan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta publica: siempre crea un CLIENTE. El rol no se recibe desde el navegador. */
public record RegistroRequest(
    @NotBlank(message = "es obligatorio")
    @Size(min = 2, max = 80, message = "debe tener entre 2 y 80 caracteres") String nombre,
    @NotBlank(message = "es obligatorio")
    @Email(message = "formato de correo invalido")
    @Size(max = 200, message = "no puede superar 200 caracteres") String correo,
    @NotBlank(message = "es obligatoria")
    @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
    @Pattern(regexp = "^$|^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$",
        message = "debe ser un celular chileno (+56 9 XXXX XXXX)") String telefono
) {}
