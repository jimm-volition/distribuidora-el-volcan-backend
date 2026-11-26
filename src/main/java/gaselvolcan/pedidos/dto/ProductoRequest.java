package gaselvolcan.pedidos.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank @Size(max=20) String codigo,
    @NotBlank @Size(max=230) String nombre,
    @Size(max=1500) String descripcion,
    @NotBlank @Size(max=30) String unidad,
    @NotNull @PositiveOrZero BigDecimal precioResidencial,
    @NotNull @PositiveOrZero BigDecimal precioComercial,
    @NotNull @Min(0) Integer stock,
    @Size(max=750) String imagenUrl,
    @NotNull Long categoriaId
) {}
