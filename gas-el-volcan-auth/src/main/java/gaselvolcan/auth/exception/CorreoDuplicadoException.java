package gaselvolcan.auth.exception;

/** Registro con un correo que ya existe -> 409. */
public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String mensaje) { super(mensaje); }
}
