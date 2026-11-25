package co.edu.sena.skillup.exception;

/**
 * Se lanza cuando faltan parámetros de configuración para iniciar la aplicación.
 */
public class ConfiguracionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el parámetro faltante.
     *
     * @param mensaje descripción del problema de configuración
     */
    public ConfiguracionException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción conservando la causa original.
     *
     * @param mensaje descripción del problema de configuración
     * @param causa excepción original
     */
    public ConfiguracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
