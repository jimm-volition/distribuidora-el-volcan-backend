package gaselvolcan.pedidos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), req, Map.of());
    }
    @ExceptionHandler({ConflictoNegocioException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflicto(Exception e, HttpServletRequest req) {
        String mensaje = e instanceof ConflictoNegocioException ? e.getMessage() : "Conflicto con los datos existentes";
        return error(HttpStatus.CONFLICT, mensaje, req, Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validar(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String,String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
            campos.put(err.getField(), err.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Datos inválidos", req, campos);
    }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> entrada(Exception e, HttpServletRequest req) {
        String mensaje = e instanceof HttpMessageNotReadableException ?
            "JSON inválido o valor de enum desconocido" : e.getMessage();
        return error(HttpStatus.BAD_REQUEST, mensaje, req, Map.of());
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> acceso(AccessDeniedException e, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "No tienes permisos para esta operación", req, Map.of());
    }
    private ResponseEntity<ApiError> error(HttpStatus status, String mensaje, HttpServletRequest req, Map<String,String> detalles) {
        return ResponseEntity.status(status).body(new ApiError(LocalDateTime.now(), status.value(),
            status.getReasonPhrase(), mensaje, req.getRequestURI(), detalles));
    }
}
