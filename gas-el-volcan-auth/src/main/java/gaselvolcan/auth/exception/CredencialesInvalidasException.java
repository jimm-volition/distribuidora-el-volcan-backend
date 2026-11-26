package gaselvolcan.auth.exception;

/** Login fallido o sesion de un usuario que ya no existe / esta desactivado -> 401. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) { super(mensaje); }
}
