package co.edu.sena.skillup;

import co.edu.sena.skillup.config.ConexionBaseDatos;
import co.edu.sena.skillup.consola.Consola;
import co.edu.sena.skillup.consola.FinEntradaException;
import co.edu.sena.skillup.consola.MenuPrincipal;
import co.edu.sena.skillup.exception.ConfiguracionException;
import co.edu.sena.skillup.exception.PersistenciaException;
import co.edu.sena.skillup.repository.JdbcUsuarioRepository;
import co.edu.sena.skillup.security.CifradorContrasena;
import co.edu.sena.skillup.service.UsuarioService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Punto de arranque de la aplicación de consola del módulo de gestión de usuarios.
 *
 * <p>Arma las capas de la aplicación (consola, servicio, repositorio JDBC y conexión) y
 * muestra el menú principal.</p>
 */
public final class SkillUpApplication {

    private static final Logger LOGGER = Logger.getLogger(SkillUpApplication.class.getName());
    private static final String ARCHIVO_REGISTRO = "/logging.properties";
    private static final int CODIGO_ERROR = 1;

    private SkillUpApplication() {
        // Clase de arranque: no se instancia.
    }

    /**
     * Inicia la aplicación.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        configurarRegistro();
        Consola consola = Consola.estandar();
        try {
            ConexionBaseDatos conexionBaseDatos = ConexionBaseDatos.desdeConfiguracion();
            String servidor = conexionBaseDatos.probarConexion();
            consola.mostrar("Conexión JDBC establecida con " + servidor + " en "
                    + conexionBaseDatos.getUrlSinParametros() + " (usuario "
                    + conexionBaseDatos.getUsuario() + ").");

            UsuarioService usuarioService = new UsuarioService(
                    new JdbcUsuarioRepository(conexionBaseDatos),
                    new CifradorContrasena(),
                    Clock.systemDefaultZone());
            new MenuPrincipal(consola, usuarioService).iniciar();
        } catch (ConfiguracionException | PersistenciaException e) {
            LOGGER.log(Level.SEVERE, "La aplicación no pudo iniciar", e);
            consola.mostrarError(e.getMessage());
            System.exit(CODIGO_ERROR);
        } catch (FinEntradaException e) {
            consola.mostrar("");
            consola.mostrar("Fin de la entrada. Hasta pronto.");
        }
    }

    private static void configurarRegistro() {
        try (InputStream configuracion =
                SkillUpApplication.class.getResourceAsStream(ARCHIVO_REGISTRO)) {
            if (configuracion != null) {
                LogManager.getLogManager().readConfiguration(configuracion);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No fue posible cargar " + ARCHIVO_REGISTRO, e);
        }
    }
}
