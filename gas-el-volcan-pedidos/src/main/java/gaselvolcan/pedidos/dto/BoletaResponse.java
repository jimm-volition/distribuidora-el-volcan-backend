package gaselvolcan.pedidos.dto;

import gaselvolcan.pedidos.model.EstadoBoleta;
import gaselvolcan.pedidos.model.TipoPrecio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
    Long id, String clienteCorreo, String nombreCliente, String direccion, String comuna,
    TipoPrecio tipoPrecio, EstadoBoleta estado, LocalDateTime fecha, BigDecimal total,
    List<DetalleResponse> detalles
) {
    public record DetalleResponse(String codigoProducto, String nombreProducto,
        Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}
}
