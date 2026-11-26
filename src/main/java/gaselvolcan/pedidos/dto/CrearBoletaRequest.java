package gaselvolcan.pedidos.dto;

import gaselvolcan.pedidos.model.TipoPrecio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CrearBoletaRequest(
    @NotBlank @Size(max=150) String nombreCliente,
    @NotBlank @Size(max=300) String direccion,
    @NotBlank @Size(max=100) String comuna,
    @NotNull TipoPrecio tipoPrecio,
    @NotEmpty List<@Valid ItemPedidoRequest> detalles
) {
    public record ItemPedidoRequest(
        @NotBlank String codigoProducto,
        @NotNull @Min(1) Integer cantidad
    ) {}
}
