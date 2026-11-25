package co.edu.sena.skillup.exception;

/**
 * Excepción base para las reglas de negocio que se informan al usuario final.
 */
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje dirigido al usuario.
     *
     * @param mensaje descripción del problema
     */
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
