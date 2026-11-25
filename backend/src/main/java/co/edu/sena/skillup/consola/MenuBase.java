package co.edu.sena.skillup.consola;

import co.edu.sena.skillup.exception.DatosInvalidosException;
import co.edu.sena.skillup.exception.NegocioException;
import co.edu.sena.skillup.exception.PersistenciaException;
import co.edu.sena.skillup.service.UsuarioService;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Comportamiento común de los menús: dependencias compartidas y manejo de errores.
 */
public abstract class MenuBase {

    protected final Consola consola;
    protected final UsuarioService usuarioService;
    protected final VistaUsuario vistaUsuario;

    /**
     * Crea el menú con sus dependencias.
     *
     * @param consola entrada y salida de texto
     * @param usuarioService lógica de negocio del módulo de usuarios
     */
    protected MenuBase(Consola consola, UsuarioService usuarioService) {
        this.consola = Objects.requireNonNull(consola);
        this.usuarioService = Objects.requireNonNull(usuarioService);
        this.vistaUsuario = new VistaUsuario(consola);
    }

    /**
     * Ejecuta una operación y muestra los errores de negocio o de base de datos.
     *
     * @param operacion operación que se ejecuta
     */
    protected void ejecutar(Runnable operacion) {
        ejecutarConResultado(() -> {
            operacion.run();
            return Boolean.TRUE;
        });
    }

    /**
     * Ejecuta una operación que produce un resultado y muestra los errores que ocurran.
     *
     * @param operacion operación que se ejecuta
     * @param <T> tipo del resultado
     * @return resultado de la operación o vacío si hubo un error
     */
    protected <T> Optional<T> ejecutarConResultado(Supplier<T> operacion) {
        try {
            return Optional.ofNullable(operacion.get());
        } catch (DatosInvalidosException e) {
            consola.mostrarError("Revise los datos ingresados:");
            e.getErrores().forEach(error -> consola.mostrar("   - " + error));
        } catch (NegocioException e) {
            consola.mostrarError(e.getMessage());
        } catch (PersistenciaException e) {
            consola.mostrarError(e.getMessage()
                    + " El detalle técnico quedó en el archivo skillup-usuarios.log.");
        }
        return Optional.empty();
    }
}
