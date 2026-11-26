package gaselvolcan.pedidos.dto;

import java.math.BigDecimal;

public record ProductoResponse(
    String codigo, String nombre, String descripcion, String unidad,
    BigDecimal precioResidencial, BigDecimal precioComercial,
    Integer stock, String imagenUrl, boolean activo,
    Long categoriaId, String categoria
) {}
