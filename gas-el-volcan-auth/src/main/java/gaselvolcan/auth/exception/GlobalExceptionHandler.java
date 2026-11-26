package gaselvolcan.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> credenciales(CredencialesInvalidasException e, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), req, Map.of());
    }

    // DataIntegrityViolation cubre dos registros simultaneos con el mismo correo (indice unico).
    @ExceptionHandler({CorreoDuplicadoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> duplicado(Exception e, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "Ese correo ya esta registrado", req, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validar(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err -> campos.putIfAbsent(err.getField(), err.getDefaultMessage()));
        String resumen = campos.entrySet().stream()
            .map(c -> c.getKey() + ": " + c.getValue())
            .reduce((a, b) -> a + ", " + b).orElse("Datos invalidos");
        return error(HttpStatus.BAD_REQUEST, resumen, req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido", req, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String mensaje, HttpServletRequest req,
                                           Map<String, String> detalles) {
        return ResponseEntity.status(status).body(new ApiError(LocalDateTime.now(), status.value(),
            status.getReasonPhrase(), mensaje, req.getRequestURI(), detalles));
    }
}
