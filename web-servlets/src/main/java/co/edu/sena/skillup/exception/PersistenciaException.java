package co.edu.sena.skillup.exception;

/**
 * Envuelve los errores de JDBC en una excepción no verificada.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción conservando la causa original.
     *
     * @param mensaje descripción de la operación que falló
     * @param causa excepción original de JDBC
     */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
