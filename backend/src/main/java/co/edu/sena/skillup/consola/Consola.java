package co.edu.sena.skillup.consola;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Entrada y salida de texto de la aplicación de escritorio en modo consola.
 *
 * <p>Es la única clase que escribe en la salida estándar. Cuando la aplicación se
 * ejecuta en una terminal real, las contraseñas se leen sin mostrarse en pantalla.</p>
 */
public class Consola {

    private static final int ANCHO_TITULO = 64;
    private static final String RESPUESTA_SI = "s";
    private static final String RESPUESTA_NO = "n";

    private final BufferedReader lector;
    private final PrintStream salida;
    private final java.io.Console terminal;

    /**
     * Crea una consola sobre los flujos recibidos; las contraseñas se leen como texto normal.
     *
     * @param entrada flujo de entrada
     * @param salida flujo de salida
     */
    public Consola(InputStream entrada, PrintStream salida) {
        this(entrada, salida, null);
    }

    private Consola(InputStream entrada, PrintStream salida, java.io.Console terminal) {
        Charset codificacion = terminal == null ? StandardCharsets.UTF_8 : terminal.charset();
        this.lector = new BufferedReader(new InputStreamReader(entrada, codificacion));
        this.salida = salida;
        this.terminal = terminal;
    }

    /**
     * Crea la consola asociada a la entrada y salida estándar del proceso.
     *
     * @return consola del sistema
     */
    public static Consola estandar() {
        return new Consola(System.in, System.out, System.console());
    }

    /**
     * Muestra una línea de texto.
     *
     * @param texto texto que se muestra
     */
    public void mostrar(String texto) {
        salida.println(texto);
    }

    /**
     * Muestra un título enmarcado.
     *
     * @param titulo texto del título
     */
    public void mostrarTitulo(String titulo) {
        String linea = "=".repeat(ANCHO_TITULO);
        salida.println();
        salida.println(linea);
        salida.println(" " + titulo);
        salida.println(linea);
    }

    /**
     * Muestra un mensaje de operación exitosa.
     *
     * @param mensaje texto del mensaje
     */
    public void mostrarExito(String mensaje) {
        salida.println("[OK] " + mensaje);
    }

    /**
     * Muestra un mensaje de error.
     *
     * @param mensaje texto del mensaje
     */
    public void mostrarError(String mensaje) {
        salida.println("[ERROR] " + mensaje);
    }

    /**
     * Lee una línea de texto sin espacios al inicio ni al final.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @return texto digitado; puede estar vacío
     * @throws FinEntradaException si la entrada estándar se cerró
     */
    public String leerTexto(String etiqueta) {
        salida.print(etiqueta + ": ");
        salida.flush();
        try {
            String linea = lector.readLine();
            if (linea == null) {
                throw new FinEntradaException();
            }
            return linea.trim();
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible leer la entrada", e);
        }
    }

    /**
     * Lee un texto mostrando el valor actual; si se presiona Enter se conserva ese valor.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @param valorActual valor que se conserva si no se digita nada
     * @return texto digitado o el valor actual
     */
    public String leerTextoConValorActual(String etiqueta, String valorActual) {
        String valor = leerTexto(etiqueta + " [" + valorActual + "]");
        return valor.isEmpty() ? valorActual : valor;
    }

    /**
     * Lee una contraseña; en una terminal real no se muestra lo que se digita.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @return contraseña digitada
     */
    public String leerContrasena(String etiqueta) {
        if (terminal == null) {
            return leerTexto(etiqueta);
        }
        salida.flush();
        char[] contrasena = terminal.readPassword("%s: ", etiqueta);
        if (contrasena == null) {
            throw new FinEntradaException();
        }
        return new String(contrasena);
    }

    /**
     * Lee un número entero dentro de un rango; repite la pregunta si el valor no es válido.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @param minimo valor mínimo aceptado
     * @param maximo valor máximo aceptado
     * @return número digitado
     */
    public int leerOpcion(String etiqueta, int minimo, int maximo) {
        while (true) {
            String texto = leerTexto(etiqueta);
            try {
                int opcion = Integer.parseInt(texto);
                if (opcion >= minimo && opcion <= maximo) {
                    return opcion;
                }
            } catch (NumberFormatException e) {
                // Se informa abajo y se vuelve a preguntar.
            }
            mostrarError("Digite un número entre " + minimo + " y " + maximo + ".");
        }
    }

    /**
     * Lee un identificador numérico positivo; repite la pregunta si el valor no es válido.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @return identificador digitado
     */
    public long leerIdentificador(String etiqueta) {
        while (true) {
            String texto = leerTexto(etiqueta);
            try {
                long identificador = Long.parseLong(texto);
                if (identificador > 0) {
                    return identificador;
                }
            } catch (NumberFormatException e) {
                // Se informa abajo y se vuelve a preguntar.
            }
            mostrarError("Digite un número entero mayor que cero.");
        }
    }

    /**
     * Pide confirmación al usuario con las respuestas s o n.
     *
     * @param pregunta pregunta que se muestra
     * @return {@code true} si el usuario responde s
     */
    public boolean confirmar(String pregunta) {
        while (true) {
            String respuesta = leerTexto(pregunta + " (s/n)").toLowerCase(Locale.ROOT);
            if (RESPUESTA_SI.equals(respuesta)) {
                return true;
            }
            if (RESPUESTA_NO.equals(respuesta)) {
                return false;
            }
            mostrarError("Responda s o n.");
        }
    }
}
