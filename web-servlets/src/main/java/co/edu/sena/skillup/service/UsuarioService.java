package co.edu.sena.skillup.service;

import co.edu.sena.skillup.dto.ActualizacionUsuarioDTO;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import co.edu.sena.skillup.exception.CredencialesInvalidasException;
import co.edu.sena.skillup.exception.CuentaBloqueadaException;
import co.edu.sena.skillup.exception.DatosInvalidosException;
import co.edu.sena.skillup.exception.OperacionNoPermitidaException;
import co.edu.sena.skillup.exception.RecursoNoEncontradoException;
import co.edu.sena.skillup.exception.RegistroDuplicadoException;
import co.edu.sena.skillup.repository.UsuarioRepository;
import co.edu.sena.skillup.security.CifradorContrasena;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Lógica de negocio del módulo de gestión de usuarios.
 *
 * <p>Cubre RF-01 Registro (HU-01), RF-02 Inicio de sesión (HU-02), RF-03 Perfil y
 * RF-09 Gestión de usuarios por el administrador (HU-07). La autorización por rol se
 * aplica en la capa de presentación; en el Sprint 2 se reemplaza por Spring Security.</p>
 */
public class UsuarioService {

    public static final int MAXIMO_INTENTOS_FALLIDOS = 5;
    public static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarioRepository;
    private final CifradorContrasena cifradorContrasena;
    private final Clock reloj;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param usuarioRepository repositorio de usuarios
     * @param cifradorContrasena componente que cifra y verifica contraseñas
     * @param reloj reloj del sistema; en las pruebas se usa uno controlado
     */
    public UsuarioService(
            UsuarioRepository usuarioRepository,
            CifradorContrasena cifradorContrasena,
            Clock reloj) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.cifradorContrasena = Objects.requireNonNull(cifradorContrasena);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /**
     * Indica si existe al menos un administrador con la cuenta activa.
     *
     * @return {@code true} si hay un administrador activo
     */
    public boolean existeAdministradorActivo() {
        return usuarioRepository.contarActivosPorRol(NombreRol.ADMINISTRADOR) > 0;
    }

    /**
     * Crea la primera cuenta de administrador cuando la base de datos aún no tiene uno.
     *
     * @param datos datos de registro del administrador
     * @return administrador creado
     * @throws OperacionNoPermitidaException si ya existe un administrador activo
     */
    public UsuarioDTO registrarAdministradorInicial(RegistroUsuarioDTO datos) {
        if (existeAdministradorActivo()) {
            throw new OperacionNoPermitidaException("Ya existe un administrador activo.");
        }
        return registrar(datos, NombreRol.ADMINISTRADOR);
    }

    /**
     * Registra una cuenta de estudiante desde el formulario público (RF-01, HU-01).
     *
     * @param datos datos de registro
     * @return estudiante creado
     * @throws DatosInvalidosException si los datos no cumplen las validaciones
     * @throws RegistroDuplicadoException si el correo o el documento ya existen
     */
    public UsuarioDTO registrarEstudiante(RegistroUsuarioDTO datos) {
        return registrar(datos, NombreRol.ESTUDIANTE);
    }

    /**
     * Crea una cuenta con el rol que elija el administrador (RF-09, HU-07).
     *
     * @param datos datos de registro
     * @param rolInicial rol que se asigna a la cuenta nueva
     * @return usuario creado
     */
    public UsuarioDTO crearUsuario(RegistroUsuarioDTO datos, NombreRol rolInicial) {
        return registrar(datos, Objects.requireNonNull(rolInicial, "El rol es obligatorio"));
    }

    /**
     * Valida las credenciales de un usuario (RF-02, HU-02).
     *
     * <p>Después de {@value #MAXIMO_INTENTOS_FALLIDOS} intentos fallidos consecutivos la
     * cuenta se bloquea durante {@value #MINUTOS_BLOQUEO} minutos.</p>
     *
     * @param correoElectronico correo de inicio de sesión
     * @param contrasena contraseña en texto plano
     * @return usuario autenticado
     * @throws CredencialesInvalidasException si el correo o la contraseña no coinciden
     * @throws CuentaBloqueadaException si la cuenta está bloqueada temporalmente
     * @throws OperacionNoPermitidaException si la cuenta está desactivada
     */
    public UsuarioDTO iniciarSesion(String correoElectronico, String contrasena) {
        if (esVacio(correoElectronico) || esVacio(contrasena)) {
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarioRepository
                .buscarPorCorreo(ValidadorUsuario.normalizarCorreo(correoElectronico))
                .orElseThrow(CredencialesInvalidasException::new);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueadaException(usuario.getBloqueadoHasta());
        }
        if (!cifradorContrasena.verificar(contrasena, usuario.getContrasenaHash())) {
            throw registrarIntentoFallido(usuario, ahora);
        }
        if (!usuario.isActivo()) {
            throw new OperacionNoPermitidaException(
                    "La cuenta está desactivada. Comuníquese con el administrador.");
        }
        if (usuario.getIntentosFallidos() > 0 || usuario.getBloqueadoHasta() != null) {
            usuarioRepository.actualizarControlAcceso(usuario.getIdUsuario(), 0, null);
        }
        return UsuarioDTO.desdeEntidad(usuario);
    }

    /**
     * Consulta todos los usuarios registrados.
     *
     * @return usuarios ordenados por identificador
     */
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.listarTodos().stream().map(UsuarioDTO::desdeEntidad).toList();
    }

    /**
     * Busca usuarios por nombre, apellido, correo o número de documento.
     *
     * @param texto texto que se busca; si está vacío se listan todos
     * @return usuarios que coinciden con la búsqueda
     */
    public List<UsuarioDTO> buscarUsuarios(String texto) {
        if (esVacio(texto)) {
            return listarUsuarios();
        }
        return usuarioRepository.buscarPorTexto(texto.trim()).stream()
                .map(UsuarioDTO::desdeEntidad)
                .toList();
    }

    /**
     * Consulta un usuario por su identificador.
     *
     * @param idUsuario identificador del usuario
     * @return usuario encontrado
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    public UsuarioDTO consultarPorId(long idUsuario) {
        return UsuarioDTO.desdeEntidad(obtenerUsuario(idUsuario));
    }

    /**
     * Actualiza los datos personales y el correo de un usuario (RF-03, RF-09).
     *
     * @param idUsuario identificador del usuario
     * @param datos datos nuevos
     * @return usuario actualizado
     * @throws DatosInvalidosException si los datos no cumplen las validaciones
     * @throws RegistroDuplicadoException si el correo o el documento pertenecen a otro usuario
     */
    public UsuarioDTO actualizarDatos(long idUsuario, ActualizacionUsuarioDTO datos) {
        Usuario usuario = obtenerUsuario(idUsuario);
        ActualizacionUsuarioDTO normalizados = ValidadorUsuario.normalizar(datos);
        lanzarSiHayErrores(ValidadorUsuario.validarActualizacion(normalizados));
        verificarDatosUnicos(
                normalizados.correoElectronico(),
                normalizados.tipoDocumento(),
                normalizados.numeroDocumento(),
                idUsuario);

        usuario.setTipoDocumento(normalizados.tipoDocumento());
        usuario.setNumeroDocumento(normalizados.numeroDocumento());
        usuario.setNombres(normalizados.nombres());
        usuario.setApellidos(normalizados.apellidos());
        usuario.setCorreoElectronico(normalizados.correoElectronico());
        usuarioRepository.actualizarDatos(usuario);
        return consultarPorId(idUsuario);
    }

    /**
     * Cambia la contraseña de un usuario después de verificar la actual (RF-03).
     *
     * @param idUsuario identificador del usuario
     * @param contrasenaActual contraseña vigente
     * @param contrasenaNueva contraseña nueva que cumple la política de HU-01
     * @throws DatosInvalidosException si la contraseña actual no coincide o la nueva es débil
     */
    public void cambiarContrasena(long idUsuario, String contrasenaActual, String contrasenaNueva) {
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!cifradorContrasena.verificar(contrasenaActual, usuario.getContrasenaHash())) {
            throw new DatosInvalidosException(List.of("La contraseña actual no es correcta."));
        }
        lanzarSiHayErrores(ValidadorUsuario.validarContrasena(contrasenaNueva));
        if (contrasenaNueva.equals(contrasenaActual)) {
            throw new DatosInvalidosException(
                    List.of("La contraseña nueva debe ser diferente de la actual."));
        }
        String contrasenaHash = cifradorContrasena.cifrar(contrasenaNueva);
        usuarioRepository.actualizarContrasena(idUsuario, contrasenaHash);
    }

    /**
     * Activa o desactiva una cuenta (RF-09, HU-07). Al activarla también se levanta el
     * bloqueo por intentos fallidos.
     *
     * @param idUsuario usuario cuyo estado cambia
     * @param activo nuevo estado
     * @param idAdministrador administrador que realiza la operación
     * @return usuario actualizado
     * @throws OperacionNoPermitidaException si el administrador intenta desactivarse a sí mismo
     */
    public UsuarioDTO cambiarEstado(long idUsuario, boolean activo, long idAdministrador) {
        if (!activo && idUsuario == idAdministrador) {
            throw new OperacionNoPermitidaException("No puede desactivar su propia cuenta.");
        }
        obtenerUsuario(idUsuario);
        usuarioRepository.actualizarEstado(idUsuario, activo);
        if (activo) {
            usuarioRepository.actualizarControlAcceso(idUsuario, 0, null);
        }
        return consultarPorId(idUsuario);
    }

    /**
     * Asigna un rol adicional a un usuario (RF-09, HU-07).
     *
     * @param idUsuario identificador del usuario
     * @param rol rol que se asigna
     * @return usuario actualizado
     * @throws OperacionNoPermitidaException si el usuario ya tiene el rol
     */
    public UsuarioDTO asignarRol(long idUsuario, NombreRol rol) {
        Usuario usuario = obtenerUsuario(idUsuario);
        if (usuario.tieneRol(rol)) {
            throw new OperacionNoPermitidaException(
                    "El usuario ya tiene el rol " + rol.getEtiqueta() + ".");
        }
        usuarioRepository.asignarRol(idUsuario, rol);
        return consultarPorId(idUsuario);
    }

    /**
     * Retira un rol de un usuario (RF-09, HU-07).
     *
     * @param idUsuario identificador del usuario
     * @param rol rol que se retira
     * @param idAdministrador administrador que realiza la operación
     * @return usuario actualizado
     * @throws OperacionNoPermitidaException si el usuario quedaría sin roles o si el
     *         administrador intenta quitarse su propio rol de administrador
     */
    public UsuarioDTO quitarRol(long idUsuario, NombreRol rol, long idAdministrador) {
        Usuario usuario = obtenerUsuario(idUsuario);
        if (!usuario.tieneRol(rol)) {
            throw new OperacionNoPermitidaException(
                    "El usuario no tiene el rol " + rol.getEtiqueta() + ".");
        }
        if (usuario.getRoles().size() == 1) {
            throw new OperacionNoPermitidaException("El usuario debe conservar al menos un rol.");
        }
        if (rol == NombreRol.ADMINISTRADOR && idUsuario == idAdministrador) {
            throw new OperacionNoPermitidaException(
                    "No puede quitarse su propio rol de administrador.");
        }
        usuarioRepository.quitarRol(idUsuario, rol);
        return consultarPorId(idUsuario);
    }

    /**
     * Elimina definitivamente una cuenta de usuario (RF-09, HU-07).
     *
     * @param idUsuario usuario que se elimina
     * @param idAdministrador administrador que realiza la operación
     * @throws OperacionNoPermitidaException si el administrador intenta eliminarse a sí mismo
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    public void eliminarUsuario(long idUsuario, long idAdministrador) {
        if (idUsuario == idAdministrador) {
            throw new OperacionNoPermitidaException("No puede eliminar su propia cuenta.");
        }
        obtenerUsuario(idUsuario);
        usuarioRepository.eliminar(idUsuario);
    }

    private UsuarioDTO registrar(RegistroUsuarioDTO datosOriginales, NombreRol rol) {
        RegistroUsuarioDTO datos = ValidadorUsuario.normalizar(
                Objects.requireNonNull(datosOriginales, "Los datos de registro son obligatorios"));
        lanzarSiHayErrores(ValidadorUsuario.validarRegistro(datos));
        verificarDatosUnicos(
                datos.correoElectronico(), datos.tipoDocumento(), datos.numeroDocumento(), null);

        Usuario usuario = new Usuario();
        usuario.setTipoDocumento(datos.tipoDocumento());
        usuario.setNumeroDocumento(datos.numeroDocumento());
        usuario.setNombres(datos.nombres());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreoElectronico(datos.correoElectronico());
        usuario.setContrasenaHash(cifradorContrasena.cifrar(datos.contrasena()));
        usuario.setActivo(true);
        usuario.agregarRol(rol);
        return UsuarioDTO.desdeEntidad(usuarioRepository.guardar(usuario));
    }

    /**
     * Suma un intento fallido y bloquea la cuenta al llegar al máximo permitido.
     *
     * @return excepción que el llamador debe lanzar
     */
    private RuntimeException registrarIntentoFallido(Usuario usuario, LocalDateTime ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAXIMO_INTENTOS_FALLIDOS) {
            LocalDateTime bloqueadoHasta = ahora.plusMinutes(MINUTOS_BLOQUEO);
            usuarioRepository.actualizarControlAcceso(usuario.getIdUsuario(), 0, bloqueadoHasta);
            return new CuentaBloqueadaException(bloqueadoHasta);
        }
        usuarioRepository.actualizarControlAcceso(usuario.getIdUsuario(), intentos, null);
        return new CredencialesInvalidasException();
    }

    private void verificarDatosUnicos(
            String correoElectronico,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            Long idUsuarioExcluido) {
        if (usuarioRepository.existeCorreo(correoElectronico, idUsuarioExcluido)) {
            throw new RegistroDuplicadoException(
                    "El correo electrónico " + correoElectronico + " ya está registrado.");
        }
        if (usuarioRepository.existeDocumento(tipoDocumento, numeroDocumento, idUsuarioExcluido)) {
            throw new RegistroDuplicadoException(
                    "Ya existe un usuario con el documento " + tipoDocumento + " "
                    + numeroDocumento + ".");
        }
    }

    private Usuario obtenerUsuario(long idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con el identificador " + idUsuario + "."));
    }

    private static void lanzarSiHayErrores(List<String> errores) {
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(errores);
        }
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
