package co.edu.sena.skillup.exception;

/**
 * Se lanza cuando se consulta un registro que no existe en la base de datos.
 */
public class RecursoNoEncontradoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con el detalle del recurso buscado.
     *
     * @param mensaje descripción del recurso inexistente
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
