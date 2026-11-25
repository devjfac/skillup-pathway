package co.edu.sena.skillup.repository;

import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones de persistencia de las tablas {@code usuario} y {@code usuario_rol}.
 *
 * <p>Agrupa las cuatro operaciones CRUD: inserción ({@link #guardar}), consulta
 * ({@link #buscarPorId}, {@link #buscarPorCorreo}, {@link #listarTodos},
 * {@link #buscarPorTexto}), actualización ({@link #actualizarDatos},
 * {@link #actualizarContrasena}, {@link #actualizarEstado}, {@link #asignarRol}) y
 * eliminación ({@link #eliminar}, {@link #quitarRol}).</p>
 */
public interface UsuarioRepository {

    /**
     * Inserta un usuario nuevo junto con sus roles en una sola transacción.
     *
     * @param usuario usuario sin identificador, con al menos un rol
     * @return usuario persistido, con identificador y fechas asignadas por la base de datos
     */
    Usuario guardar(Usuario usuario);

    /**
     * Consulta un usuario por su llave primaria.
     *
     * @param idUsuario identificador del usuario
     * @return usuario encontrado o vacío si no existe
     */
    Optional<Usuario> buscarPorId(long idUsuario);

    /**
     * Consulta un usuario por su correo electrónico.
     *
     * @param correoElectronico correo normalizado en minúsculas
     * @return usuario encontrado o vacío si no existe
     */
    Optional<Usuario> buscarPorCorreo(String correoElectronico);

    /**
     * Consulta todos los usuarios ordenados por identificador.
     *
     * @return lista de usuarios; vacía si no hay registros
     */
    List<Usuario> listarTodos();

    /**
     * Busca usuarios cuyo nombre, apellido, correo o documento contengan el texto.
     *
     * @param texto texto que se busca
     * @return usuarios que coinciden; lista vacía si no hay coincidencias
     */
    List<Usuario> buscarPorTexto(String texto);

    /**
     * Indica si el correo ya está registrado por un usuario distinto del excluido.
     *
     * @param correoElectronico correo que se verifica
     * @param idUsuarioExcluido usuario que se ignora en la búsqueda; {@code null} para ninguno
     * @return {@code true} si otro usuario ya usa el correo
     */
    boolean existeCorreo(String correoElectronico, Long idUsuarioExcluido);

    /**
     * Indica si el documento ya está registrado por un usuario distinto del excluido.
     *
     * @param tipoDocumento tipo de documento
     * @param numeroDocumento número de documento
     * @param idUsuarioExcluido usuario que se ignora en la búsqueda; {@code null} para ninguno
     * @return {@code true} si otro usuario ya usa el documento
     */
    boolean existeDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento, Long idUsuarioExcluido);

    /**
     * Cuenta los usuarios activos que tienen el rol indicado.
     *
     * @param rol rol que se cuenta
     * @return cantidad de usuarios activos con el rol
     */
    long contarActivosPorRol(NombreRol rol);

    /**
     * Actualiza los datos personales y el correo de un usuario.
     *
     * @param usuario usuario con identificador y datos nuevos
     * @return {@code true} si el registro existía y fue actualizado
     */
    boolean actualizarDatos(Usuario usuario);

    /**
     * Reemplaza el hash de la contraseña de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @param contrasenaHash nuevo hash de la contraseña
     * @return {@code true} si el registro existía y fue actualizado
     */
    boolean actualizarContrasena(long idUsuario, String contrasenaHash);

    /**
     * Activa o desactiva la cuenta de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @param activo nuevo estado de la cuenta
     * @return {@code true} si el registro existía y fue actualizado
     */
    boolean actualizarEstado(long idUsuario, boolean activo);

    /**
     * Actualiza el contador de intentos fallidos y la fecha de bloqueo (HU-02, CA-03).
     *
     * @param idUsuario identificador del usuario
     * @param intentosFallidos intentos fallidos consecutivos
     * @param bloqueadoHasta fin del bloqueo temporal; {@code null} si no hay bloqueo
     * @return {@code true} si el registro existía y fue actualizado
     */
    boolean actualizarControlAcceso(
            long idUsuario, int intentosFallidos, LocalDateTime bloqueadoHasta);

    /**
     * Asigna un rol a un usuario existente.
     *
     * @param idUsuario identificador del usuario
     * @param rol rol que se asigna
     * @return {@code true} si se insertó la asignación
     */
    boolean asignarRol(long idUsuario, NombreRol rol);

    /**
     * Retira un rol de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @param rol rol que se retira
     * @return {@code true} si se eliminó la asignación
     */
    boolean quitarRol(long idUsuario, NombreRol rol);

    /**
     * Elimina un usuario; sus asignaciones de rol se eliminan en cascada.
     *
     * @param idUsuario identificador del usuario
     * @return {@code true} si el registro existía y fue eliminado
     */
    boolean eliminar(long idUsuario);
}
