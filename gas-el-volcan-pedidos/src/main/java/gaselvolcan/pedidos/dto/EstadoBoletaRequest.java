package gaselvolcan.pedidos.dto;

import gaselvolcan.pedidos.model.EstadoBoleta;
import jakarta.validation.constraints.NotNull;
public record EstadoBoletaRequest(@NotNull EstadoBoleta estado) {}
