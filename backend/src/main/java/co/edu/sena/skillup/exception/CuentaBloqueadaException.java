package co.edu.sena.skillup.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Se lanza cuando la cuenta está bloqueada por intentos fallidos (HU-02, CA-03).
 */
public class CuentaBloqueadaException extends NegocioException {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDateTime bloqueadoHasta;

    /**
     * Crea la excepción indicando hasta cuándo dura el bloqueo.
     *
     * @param bloqueadoHasta fecha y hora en que la cuenta vuelve a estar disponible
     */
    public CuentaBloqueadaException(LocalDateTime bloqueadoHasta) {
        super("La cuenta está bloqueada temporalmente por intentos fallidos. "
                + "Intente de nuevo después de las " + bloqueadoHasta.format(FORMATO_HORA) + ".");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
