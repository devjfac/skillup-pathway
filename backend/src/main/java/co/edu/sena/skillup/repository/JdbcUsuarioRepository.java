package co.edu.sena.skillup.repository;

import co.edu.sena.skillup.config.ConexionBaseDatos;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import co.edu.sena.skillup.exception.PersistenciaException;
import co.edu.sena.skillup.exception.RegistroDuplicadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación JDBC del repositorio de usuarios sobre MySQL 8.
 *
 * <p>Cada operación abre su propia conexión con try-with-resources, usa
 * {@link PreparedStatement} con parámetros para evitar la inyección SQL y traduce las
 * {@link SQLException} a excepciones no verificadas del proyecto. La inserción del
 * usuario y de sus roles se ejecuta en una transacción explícita.</p>
 */
public class JdbcUsuarioRepository implements UsuarioRepository {

    private static final Logger LOGGER = Logger.getLogger(JdbcUsuarioRepository.class.getName());

    /** Código de error de MySQL para una llave única duplicada (ER_DUP_ENTRY). */
    private static final int CODIGO_ERROR_DUPLICADO = 1062;
    private static final String SEPARADOR_ROLES = ",";
    private static final long SIN_USUARIO_EXCLUIDO = 0L;

    private static final String SQL_SELECCION_BASE = """
            SELECT
                u.id_usuario,
                u.tipo_documento,
                u.numero_documento,
                u.nombres,
                u.apellidos,
                u.correo_electronico,
                u.contrasena_hash,
                u.activo,
                u.intentos_fallidos,
                u.bloqueado_hasta,
                u.fecha_creacion,
                u.fecha_actualizacion,
                GROUP_CONCAT(r.nombre_rol ORDER BY r.id_rol SEPARATOR ',') AS roles
            FROM usuario AS u
            LEFT JOIN usuario_rol AS ur
                ON ur.id_usuario = u.id_usuario
            LEFT JOIN rol AS r
                ON r.id_rol = ur.id_rol
            """;

    private static final String SQL_BUSCAR_POR_ID = SQL_SELECCION_BASE + """
            WHERE u.id_usuario = ?
            GROUP BY u.id_usuario
            """;

    private static final String SQL_BUSCAR_POR_CORREO = SQL_SELECCION_BASE + """
            WHERE u.correo_electronico = ?
            GROUP BY u.id_usuario
            """;

    private static final String SQL_LISTAR_TODOS = SQL_SELECCION_BASE + """
            GROUP BY u.id_usuario
            ORDER BY u.id_usuario
            """;

    private static final String SQL_BUSCAR_POR_TEXTO = SQL_SELECCION_BASE + """
            WHERE u.nombres LIKE ?
               OR u.apellidos LIKE ?
               OR u.correo_electronico LIKE ?
               OR u.numero_documento LIKE ?
            GROUP BY u.id_usuario
            ORDER BY u.apellidos, u.nombres
            """;

    private static final String SQL_INSERTAR_USUARIO = """
            INSERT INTO usuario (
                tipo_documento,
                numero_documento,
                nombres,
                apellidos,
                correo_electronico,
                contrasena_hash,
                activo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_ASIGNAR_ROL = """
            INSERT INTO usuario_rol (id_usuario, id_rol)
            SELECT ?, r.id_rol
            FROM rol AS r
            WHERE r.nombre_rol = ?
            """;

    private static final String SQL_QUITAR_ROL = """
            DELETE ur
            FROM usuario_rol AS ur
            INNER JOIN rol AS r
                ON r.id_rol = ur.id_rol
            WHERE ur.id_usuario = ?
              AND r.nombre_rol = ?
            """;

    private static final String SQL_EXISTE_CORREO = """
            SELECT 1
            FROM usuario
            WHERE correo_electronico = ?
              AND id_usuario <> ?
            LIMIT 1
            """;

    private static final String SQL_EXISTE_DOCUMENTO = """
            SELECT 1
            FROM usuario
            WHERE tipo_documento = ?
              AND numero_documento = ?
              AND id_usuario <> ?
            LIMIT 1
            """;

    private static final String SQL_CONTAR_ACTIVOS_POR_ROL = """
            SELECT COUNT(ur.id_usuario_rol) AS total
            FROM usuario_rol AS ur
            INNER JOIN rol AS r
                ON r.id_rol = ur.id_rol
            INNER JOIN usuario AS u
                ON u.id_usuario = ur.id_usuario
            WHERE r.nombre_rol = ?
              AND u.activo = 1
            """;

    private static final String SQL_ACTUALIZAR_DATOS = """
            UPDATE usuario
            SET tipo_documento = ?,
                numero_documento = ?,
                nombres = ?,
                apellidos = ?,
                correo_electronico = ?
            WHERE id_usuario = ?
            """;

    private static final String SQL_ACTUALIZAR_CONTRASENA = """
            UPDATE usuario
            SET contrasena_hash = ?
            WHERE id_usuario = ?
            """;

    private static final String SQL_ACTUALIZAR_ESTADO = """
            UPDATE usuario
            SET activo = ?
            WHERE id_usuario = ?
            """;

    private static final String SQL_ACTUALIZAR_CONTROL_ACCESO = """
            UPDATE usuario
            SET intentos_fallidos = ?,
                bloqueado_hasta = ?
            WHERE id_usuario = ?
            """;

    private static final String SQL_ELIMINAR = """
            DELETE FROM usuario
            WHERE id_usuario = ?
            """;

    private final ConexionBaseDatos conexionBaseDatos;

    /**
     * Crea el repositorio con la fábrica de conexiones JDBC.
     *
     * @param conexionBaseDatos fábrica de conexiones hacia MySQL
     */
    public JdbcUsuarioRepository(ConexionBaseDatos conexionBaseDatos) {
        this.conexionBaseDatos = Objects.requireNonNull(conexionBaseDatos);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario.getRoles().isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol");
        }
        long idGenerado;
        try (Connection conexion = conexionBaseDatos.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                idGenerado = insertarUsuario(conexion, usuario);
                for (NombreRol rol : usuario.getRoles()) {
                    if (insertarRol(conexion, idGenerado, rol) == 0) {
                        throw new SQLException("El rol " + rol + " no existe en la tabla rol");
                    }
                }
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw traducirExcepcion("registrar el usuario", e);
        }
        return buscarPorId(idGenerado).orElseThrow(() -> new PersistenciaException(
                "El usuario " + idGenerado + " no se encontró después de guardarlo", null));
    }

    @Override
    public Optional<Usuario> buscarPorId(long idUsuario) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            sentencia.setLong(1, idUsuario);
            return consultarUno(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("consultar el usuario " + idUsuario, e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_CORREO)) {
            sentencia.setString(1, correoElectronico);
            return consultarUno(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("consultar el usuario por correo", e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_TODOS)) {
            return consultarVarios(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("listar los usuarios", e);
        }
    }

    @Override
    public List<Usuario> buscarPorTexto(String texto) {
        String patron = "%" + escaparComodines(texto) + "%";
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_TEXTO)) {
            for (int indice = 1; indice <= 4; indice++) {
                sentencia.setString(indice, patron);
            }
            return consultarVarios(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("buscar usuarios", e);
        }
    }

    @Override
    public boolean existeCorreo(String correoElectronico, Long idUsuarioExcluido) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_EXISTE_CORREO)) {
            sentencia.setString(1, correoElectronico);
            sentencia.setLong(2, idExcluidoOSinUsuario(idUsuarioExcluido));
            return existeResultado(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("verificar el correo electrónico", e);
        }
    }

    @Override
    public boolean existeDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento, Long idUsuarioExcluido) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_EXISTE_DOCUMENTO)) {
            sentencia.setString(1, tipoDocumento.name());
            sentencia.setString(2, numeroDocumento);
            sentencia.setLong(3, idExcluidoOSinUsuario(idUsuarioExcluido));
            return existeResultado(sentencia);
        } catch (SQLException e) {
            throw traducirExcepcion("verificar el documento", e);
        }
    }

    @Override
    public long contarActivosPorRol(NombreRol rol) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia =
                        conexion.prepareStatement(SQL_CONTAR_ACTIVOS_POR_ROL)) {
            sentencia.setString(1, rol.name());
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getLong("total") : 0L;
            }
        } catch (SQLException e) {
            throw traducirExcepcion("contar los usuarios con rol " + rol, e);
        }
    }

    @Override
    public boolean actualizarDatos(Usuario usuario) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_DATOS)) {
            sentencia.setString(1, usuario.getTipoDocumento().name());
            sentencia.setString(2, usuario.getNumeroDocumento());
            sentencia.setString(3, usuario.getNombres());
            sentencia.setString(4, usuario.getApellidos());
            sentencia.setString(5, usuario.getCorreoElectronico());
            sentencia.setLong(6, usuario.getIdUsuario());
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("actualizar el usuario " + usuario.getIdUsuario(), e);
        }
    }

    @Override
    public boolean actualizarContrasena(long idUsuario, String contrasenaHash) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia =
                        conexion.prepareStatement(SQL_ACTUALIZAR_CONTRASENA)) {
            sentencia.setString(1, contrasenaHash);
            sentencia.setLong(2, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("actualizar la contraseña del usuario " + idUsuario, e);
        }
    }

    @Override
    public boolean actualizarEstado(long idUsuario, boolean activo) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            sentencia.setBoolean(1, activo);
            sentencia.setLong(2, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("cambiar el estado del usuario " + idUsuario, e);
        }
    }

    @Override
    public boolean actualizarControlAcceso(
            long idUsuario, int intentosFallidos, LocalDateTime bloqueadoHasta) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia =
                        conexion.prepareStatement(SQL_ACTUALIZAR_CONTROL_ACCESO)) {
            sentencia.setInt(1, intentosFallidos);
            if (bloqueadoHasta == null) {
                sentencia.setNull(2, Types.TIMESTAMP);
            } else {
                sentencia.setObject(2, bloqueadoHasta);
            }
            sentencia.setLong(3, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("registrar el intento de acceso del usuario " + idUsuario, e);
        }
    }

    @Override
    public boolean asignarRol(long idUsuario, NombreRol rol) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion()) {
            return insertarRol(conexion, idUsuario, rol) > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("asignar el rol " + rol + " al usuario " + idUsuario, e);
        }
    }

    @Override
    public boolean quitarRol(long idUsuario, NombreRol rol) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_QUITAR_ROL)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, rol.name());
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("quitar el rol " + rol + " al usuario " + idUsuario, e);
        }
    }

    @Override
    public boolean eliminar(long idUsuario) {
        try (Connection conexion = conexionBaseDatos.obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
            sentencia.setLong(1, idUsuario);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducirExcepcion("eliminar el usuario " + idUsuario, e);
        }
    }

    private long insertarUsuario(Connection conexion, Usuario usuario) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                SQL_INSERTAR_USUARIO, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, usuario.getTipoDocumento().name());
            sentencia.setString(2, usuario.getNumeroDocumento());
            sentencia.setString(3, usuario.getNombres());
            sentencia.setString(4, usuario.getApellidos());
            sentencia.setString(5, usuario.getCorreoElectronico());
            sentencia.setString(6, usuario.getContrasenaHash());
            sentencia.setBoolean(7, usuario.isActivo());
            sentencia.executeUpdate();
            try (ResultSet llaves = sentencia.getGeneratedKeys()) {
                if (!llaves.next()) {
                    throw new SQLException("MySQL no devolvió el identificador generado");
                }
                return llaves.getLong(1);
            }
        }
    }

    private int insertarRol(Connection conexion, long idUsuario, NombreRol rol)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ASIGNAR_ROL)) {
            sentencia.setLong(1, idUsuario);
            sentencia.setString(2, rol.name());
            return sentencia.executeUpdate();
        }
    }

    private Optional<Usuario> consultarUno(PreparedStatement sentencia) throws SQLException {
        try (ResultSet resultado = sentencia.executeQuery()) {
            return resultado.next() ? Optional.of(mapearUsuario(resultado)) : Optional.empty();
        }
    }

    private List<Usuario> consultarVarios(PreparedStatement sentencia) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try (ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                usuarios.add(mapearUsuario(resultado));
            }
        }
        return usuarios;
    }

    private static boolean existeResultado(PreparedStatement sentencia) throws SQLException {
        try (ResultSet resultado = sentencia.executeQuery()) {
            return resultado.next();
        }
    }

    private static Usuario mapearUsuario(ResultSet resultado) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(resultado.getLong("id_usuario"));
        usuario.setTipoDocumento(TipoDocumento.valueOf(resultado.getString("tipo_documento")));
        usuario.setNumeroDocumento(resultado.getString("numero_documento"));
        usuario.setNombres(resultado.getString("nombres"));
        usuario.setApellidos(resultado.getString("apellidos"));
        usuario.setCorreoElectronico(resultado.getString("correo_electronico"));
        usuario.setContrasenaHash(resultado.getString("contrasena_hash"));
        usuario.setActivo(resultado.getBoolean("activo"));
        usuario.setIntentosFallidos(resultado.getInt("intentos_fallidos"));
        usuario.setBloqueadoHasta(resultado.getObject("bloqueado_hasta", LocalDateTime.class));
        usuario.setFechaCreacion(resultado.getObject("fecha_creacion", LocalDateTime.class));
        usuario.setFechaActualizacion(
                resultado.getObject("fecha_actualizacion", LocalDateTime.class));

        String roles = resultado.getString("roles");
        if (roles != null && !roles.isBlank()) {
            for (String rol : roles.split(SEPARADOR_ROLES)) {
                usuario.agregarRol(NombreRol.valueOf(rol.trim()));
            }
        }
        return usuario;
    }

    private static long idExcluidoOSinUsuario(Long idUsuarioExcluido) {
        return idUsuarioExcluido == null ? SIN_USUARIO_EXCLUIDO : idUsuarioExcluido;
    }

    private static String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void revertir(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException errorReversion) {
            causa.addSuppressed(errorReversion);
        }
    }

    private static RuntimeException traducirExcepcion(String operacion, SQLException e) {
        if (e.getErrorCode() == CODIGO_ERROR_DUPLICADO) {
            return new RegistroDuplicadoException(
                    "Ya existe un usuario con el mismo correo electrónico o documento.");
        }
        LOGGER.log(Level.SEVERE, "Error JDBC al " + operacion, e);
        return new PersistenciaException("No fue posible " + operacion + ".", e);
    }
}
