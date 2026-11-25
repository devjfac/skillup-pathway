package co.edu.sena.skillup.exception;

/**
 * Se lanza cuando el correo o la contraseña no coinciden (HU-02, CA-02).
 *
 * <p>El mensaje es el mismo en ambos casos para no revelar cuál de los dos datos falló.</p>
 */
public class CredencialesInvalidasException extends NegocioException {

    public static final String MENSAJE = "Correo electrónico o contraseña incorrectos.";

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el mensaje genérico de credenciales inválidas.
     */
    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}
