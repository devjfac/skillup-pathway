package co.edu.sena.skillup.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio del módulo de usuarios.
 */
public class OperacionNoPermitidaException extends NegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el motivo del rechazo.
     *
     * @param mensaje motivo por el que no se permite la operación
     */
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
