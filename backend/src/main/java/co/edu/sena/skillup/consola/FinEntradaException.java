package co.edu.sena.skillup.consola;

/**
 * Indica que la entrada estándar se cerró y la aplicación debe terminar.
 */
public class FinEntradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje fijo.
     */
    public FinEntradaException() {
        super("La entrada estándar se cerró");
    }
}
