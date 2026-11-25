package co.edu.sena.skillup.exception;

/**
 * Se lanza cuando el correo electrónico o el documento ya pertenecen a otro usuario.
 */
public class RegistroDuplicadoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el detalle del dato duplicado.
     *
     * @param mensaje descripción del dato duplicado
     */
    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
