package co.edu.sena.skillup.config;

import co.edu.sena.skillup.exception.ConfiguracionException;
import co.edu.sena.skillup.exception.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC hacia la base de datos MySQL de SkillUp Pathway.
 *
 * <p>Los parámetros se leen del archivo {@code db.properties} del classpath y pueden
 * sobrescribirse con las variables de entorno {@code SKILLUP_DB_URL} y
 * {@code SKILLUP_DB_USUARIO}. La contraseña solo se toma de la variable de entorno
 * {@code SKILLUP_DB_CONTRASENA}, de modo que nunca queda escrita en el repositorio.</p>
 */
public class ConexionBaseDatos {

    public static final String VARIABLE_URL = "SKILLUP_DB_URL";
    public static final String VARIABLE_USUARIO = "SKILLUP_DB_USUARIO";
    public static final String VARIABLE_CONTRASENA = "SKILLUP_DB_CONTRASENA";

    private static final String ARCHIVO_PROPIEDADES = "/db.properties";
    private static final String PROPIEDAD_URL = "skillup.db.url";
    private static final String PROPIEDAD_USUARIO = "skillup.db.usuario";

    private final String url;
    private final String usuario;
    private final String contrasena;

    /**
     * Crea la fábrica de conexiones con parámetros explícitos.
     *
     * @param url cadena de conexión JDBC, por ejemplo {@code jdbc:mysql://localhost:3306/skillup}
     * @param usuario usuario de la base de datos
     * @param contrasena contraseña del usuario de la base de datos
     */
    public ConexionBaseDatos(String url, String usuario, String contrasena) {
        this.url = Objects.requireNonNull(url, "La URL de conexión es obligatoria");
        this.usuario = Objects.requireNonNull(usuario, "El usuario es obligatorio");
        this.contrasena = Objects.requireNonNull(contrasena, "La contraseña es obligatoria");
    }

    /**
     * Construye la fábrica a partir de {@code db.properties} y de las variables de entorno.
     *
     * @return fábrica de conexiones lista para usar
     * @throws ConfiguracionException si falta algún parámetro de conexión
     */
    public static ConexionBaseDatos desdeConfiguracion() {
        return desdeConfiguracion(System.getenv());
    }

    /**
     * Construye la fábrica a partir de {@code db.properties} y del mapa de entorno recibido.
     *
     * @param entorno variables de entorno disponibles
     * @return fábrica de conexiones lista para usar
     * @throws ConfiguracionException si falta algún parámetro de conexión
     */
    static ConexionBaseDatos desdeConfiguracion(Map<String, String> entorno) {
        Properties propiedades = cargarPropiedades();
        String url = elegirValor(entorno.get(VARIABLE_URL), propiedades.getProperty(PROPIEDAD_URL));
        String usuario = elegirValor(
                entorno.get(VARIABLE_USUARIO), propiedades.getProperty(PROPIEDAD_USUARIO));
        String contrasena = entorno.get(VARIABLE_CONTRASENA);

        if (url == null || usuario == null) {
            throw new ConfiguracionException(
                    "Faltan las propiedades " + PROPIEDAD_URL + " o " + PROPIEDAD_USUARIO
                    + " en db.properties.");
        }
        if (contrasena == null || contrasena.isBlank()) {
            throw new ConfiguracionException(
                    "Defina la variable de entorno " + VARIABLE_CONTRASENA
                    + " con la contraseña del usuario " + usuario + " de MySQL.");
        }
        return new ConexionBaseDatos(url, usuario, contrasena);
    }

    /**
     * Abre una nueva conexión JDBC; quien la recibe es responsable de cerrarla.
     *
     * @return conexión abierta hacia MySQL
     * @throws SQLException si el servidor no está disponible o rechaza las credenciales
     */
    public Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    /**
     * Verifica que la conexión funcione y devuelve el nombre y la versión del servidor.
     *
     * @return descripción del servidor, por ejemplo {@code MySQL 8.4.3}
     * @throws PersistenciaException si no es posible conectarse
     */
    public String probarConexion() {
        try (Connection conexion = obtenerConexion()) {
            DatabaseMetaData metadatos = conexion.getMetaData();
            return metadatos.getDatabaseProductName() + " " + metadatos.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "No fue posible conectarse a " + getUrlSinParametros()
                    + " con el usuario " + usuario + ". Verifique que MySQL esté en ejecución. "
                    + "Detalle: " + e.getMessage(), e);
        }
    }

    /**
     * Devuelve la URL de conexión sin parámetros adicionales, apta para mostrarse en pantalla.
     *
     * @return URL JDBC sin la cadena de consulta
     */
    public String getUrlSinParametros() {
        int inicioParametros = url.indexOf('?');
        return inicioParametros < 0 ? url : url.substring(0, inicioParametros);
    }

    public String getUsuario() {
        return usuario;
    }

    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();
        try (InputStream entrada =
                ConexionBaseDatos.class.getResourceAsStream(ARCHIVO_PROPIEDADES)) {
            if (entrada != null) {
                propiedades.load(entrada);
            }
        } catch (IOException e) {
            throw new ConfiguracionException("No fue posible leer " + ARCHIVO_PROPIEDADES, e);
        }
        return propiedades;
    }

    private static String elegirValor(String valorEntorno, String valorArchivo) {
        if (valorEntorno != null && !valorEntorno.isBlank()) {
            return valorEntorno.trim();
        }
        return valorArchivo == null || valorArchivo.isBlank() ? null : valorArchivo.trim();
    }
}
