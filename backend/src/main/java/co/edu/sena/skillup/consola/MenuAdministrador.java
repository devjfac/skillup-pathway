package co.edu.sena.skillup.consola;

import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.service.UsuarioService;

/**
 * Panel de administración de cuentas de usuario (RF-09, HU-07).
 *
 * <p>Reúne las cuatro operaciones CRUD sobre la tabla {@code usuario}: inserción,
 * consulta, actualización y eliminación, además de la gestión de roles.</p>
 */
public class MenuAdministrador extends MenuBase {

    private static final int OPCION_CERRAR_SESION = 0;
    private static final int OPCION_LISTAR = 1;
    private static final int OPCION_BUSCAR = 2;
    private static final int OPCION_CONSULTAR = 3;
    private static final int OPCION_CREAR = 4;
    private static final int OPCION_ACTUALIZAR = 5;
    private static final int OPCION_CAMBIAR_ESTADO = 6;
    private static final int OPCION_ASIGNAR_ROL = 7;
    private static final int OPCION_QUITAR_ROL = 8;
    private static final int OPCION_ELIMINAR = 9;
    private static final int OPCION_MI_PERFIL = 10;

    private final UsuarioDTO administrador;

    /**
     * Crea el panel de administración para el administrador autenticado.
     *
     * @param consola entrada y salida de texto
     * @param usuarioService lógica de negocio del módulo de usuarios
     * @param administrador usuario administrador que inició sesión
     */
    public MenuAdministrador(
            Consola consola, UsuarioService usuarioService, UsuarioDTO administrador) {
        super(consola, usuarioService);
        this.administrador = administrador;
    }

    /**
     * Muestra el panel hasta que el administrador cierre la sesión.
     */
    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            mostrarOpciones();
            int opcion = consola.leerOpcion("Seleccione una opción", 0, OPCION_MI_PERFIL);
            switch (opcion) {
                case OPCION_LISTAR -> listarUsuarios();
                case OPCION_BUSCAR -> buscarUsuarios();
                case OPCION_CONSULTAR -> consultarUsuario();
                case OPCION_CREAR -> crearUsuario();
                case OPCION_ACTUALIZAR -> actualizarUsuario();
                case OPCION_CAMBIAR_ESTADO -> cambiarEstado();
                case OPCION_ASIGNAR_ROL -> asignarRol();
                case OPCION_QUITAR_ROL -> quitarRol();
                case OPCION_ELIMINAR -> eliminarUsuario();
                case OPCION_MI_PERFIL ->
                        new MenuPerfil(consola, usuarioService, administrador).iniciar();
                default -> continuar = false;
            }
        }
        consola.mostrar("Sesión de administrador cerrada.");
    }

    private void mostrarOpciones() {
        consola.mostrarTitulo("Administración de usuarios (RF-09) - "
                + administrador.nombreCompleto());
        consola.mostrar(" 1. Listar usuarios                       [Consulta]");
        consola.mostrar(" 2. Buscar por nombre, correo o documento [Consulta]");
        consola.mostrar(" 3. Consultar usuario por identificador   [Consulta]");
        consola.mostrar(" 4. Crear usuario con rol                 [Inserción]");
        consola.mostrar(" 5. Actualizar datos de un usuario        [Actualización]");
        consola.mostrar(" 6. Activar o desactivar una cuenta       [Actualización]");
        consola.mostrar(" 7. Asignar rol                           [Inserción]");
        consola.mostrar(" 8. Quitar rol                            [Eliminación]");
        consola.mostrar(" 9. Eliminar usuario                      [Eliminación]");
        consola.mostrar("10. Mi perfil y contraseña");
        consola.mostrar(" 0. Cerrar sesión");
    }

    private void listarUsuarios() {
        consola.mostrarTitulo("Usuarios registrados");
        ejecutarConResultado(usuarioService::listarUsuarios)
                .ifPresent(vistaUsuario::mostrarListado);
    }

    private void buscarUsuarios() {
        ejecutarConResultado(() -> usuarioService.buscarUsuarios(
                consola.leerTexto("Texto a buscar")))
                .ifPresent(vistaUsuario::mostrarListado);
    }

    private void consultarUsuario() {
        ejecutarConResultado(() -> usuarioService.consultarPorId(
                consola.leerIdentificador("Identificador del usuario")))
                .ifPresent(vistaUsuario::mostrarDetalle);
    }

    private void crearUsuario() {
        consola.mostrarTitulo("Crear usuario (RF-09) [Inserción]");
        ejecutarConResultado(() -> {
            NombreRol rolInicial = vistaUsuario.leerRol("Rol inicial");
            return usuarioService.crearUsuario(vistaUsuario.leerDatosRegistro(), rolInicial);
        }).ifPresent(usuario -> {
            consola.mostrarExito("Usuario creado.");
            vistaUsuario.mostrarDetalle(usuario);
        });
    }

    private void actualizarUsuario() {
        consola.mostrarTitulo("Actualizar usuario (RF-09) [Actualización]");
        ejecutarConResultado(() -> {
            UsuarioDTO actual = usuarioService.consultarPorId(
                    consola.leerIdentificador("Identificador del usuario"));
            vistaUsuario.mostrarDetalle(actual);
            return usuarioService.actualizarDatos(
                    actual.idUsuario(), vistaUsuario.leerDatosActualizacion(actual));
        }).ifPresent(usuario -> {
            consola.mostrarExito("Usuario actualizado.");
            vistaUsuario.mostrarDetalle(usuario);
        });
    }

    private void cambiarEstado() {
        ejecutarConResultado(() -> {
            UsuarioDTO actual = usuarioService.consultarPorId(
                    consola.leerIdentificador("Identificador del usuario"));
            boolean activar = !actual.activo();
            String accion = activar ? "activar" : "desactivar";
            if (!consola.confirmar("¿Desea " + accion + " la cuenta de "
                    + actual.nombreCompleto() + "?")) {
                consola.mostrar("Operación cancelada.");
                return null;
            }
            return usuarioService.cambiarEstado(
                    actual.idUsuario(), activar, administrador.idUsuario());
        }).ifPresent(usuario -> consola.mostrarExito("La cuenta de " + usuario.nombreCompleto()
                + " quedó " + (usuario.activo() ? "activa." : "inactiva.")));
    }

    private void asignarRol() {
        ejecutarConResultado(() -> {
            long idUsuario = consola.leerIdentificador("Identificador del usuario");
            NombreRol rol = vistaUsuario.leerRol("Rol que desea asignar");
            return usuarioService.asignarRol(idUsuario, rol);
        }).ifPresent(usuario -> {
            consola.mostrarExito("Rol asignado.");
            vistaUsuario.mostrarDetalle(usuario);
        });
    }

    private void quitarRol() {
        ejecutarConResultado(() -> {
            long idUsuario = consola.leerIdentificador("Identificador del usuario");
            NombreRol rol = vistaUsuario.leerRol("Rol que desea quitar");
            return usuarioService.quitarRol(idUsuario, rol, administrador.idUsuario());
        }).ifPresent(usuario -> {
            consola.mostrarExito("Rol retirado.");
            vistaUsuario.mostrarDetalle(usuario);
        });
    }

    private void eliminarUsuario() {
        consola.mostrarTitulo("Eliminar usuario (RF-09) [Eliminación]");
        ejecutar(() -> {
            UsuarioDTO usuario = usuarioService.consultarPorId(
                    consola.leerIdentificador("Identificador del usuario"));
            vistaUsuario.mostrarDetalle(usuario);
            if (consola.confirmar("¿Confirma la eliminación definitiva de esta cuenta?")) {
                usuarioService.eliminarUsuario(usuario.idUsuario(), administrador.idUsuario());
                consola.mostrarExito("Usuario " + usuario.idUsuario() + " eliminado.");
            } else {
                consola.mostrar("Operación cancelada.");
            }
        });
    }
}
