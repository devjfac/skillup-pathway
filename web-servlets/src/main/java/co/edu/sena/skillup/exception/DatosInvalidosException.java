package co.edu.sena.skillup.exception;

import java.util.List;

/**
 * Se lanza cuando los datos recibidos no cumplen las reglas de validación.
 */
public class DatosInvalidosException extends NegocioException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    /**
     * Crea la excepción con la lista de errores encontrados.
     *
     * @param errores mensajes de validación, uno por regla incumplida
     */
    public DatosInvalidosException(List<String> errores) {
        super("Datos inválidos: " + String.join(" ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
