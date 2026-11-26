package gaselvolcan.auth.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Mismo formato de error que gas-el-volcan-pedidos, para que React los trate igual. */
public record ApiError(LocalDateTime timestamp, int status, String error,
                       String mensaje, String ruta, Map<String, String> detalles) {}
