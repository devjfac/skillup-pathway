package co.edu.sena.skillup.consola;

import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.service.UsuarioService;

/**
 * Menú del perfil propio para estudiantes, instructores y demás roles (RF-03).
 */
public class MenuPerfil extends MenuBase {

    private static final int OPCION_CERRAR_SESION = 0;
    private static final int OPCION_VER_PERFIL = 1;
    private static final int OPCION_ACTUALIZAR_DATOS = 2;
    private static final int OPCION_CAMBIAR_CONTRASENA = 3;

    private UsuarioDTO usuarioActual;

    /**
     * Crea el menú de perfil para el usuario autenticado.
     *
     * @param consola entrada y salida de texto
     * @param usuarioService lógica de negocio del módulo de usuarios
     * @param usuarioActual usuario que inició sesión
     */
    public MenuPerfil(Consola consola, UsuarioService usuarioService, UsuarioDTO usuarioActual) {
        super(consola, usuarioService);
        this.usuarioActual = usuarioActual;
    }

    /**
     * Muestra el menú hasta que el usuario cierre la sesión.
     */
    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            consola.mostrarTitulo("Mi perfil - " + usuarioActual.nombreCompleto());
            consola.mostrar(OPCION_VER_PERFIL + ". Ver mis datos [Consulta]");
            consola.mostrar(OPCION_ACTUALIZAR_DATOS + ". Actualizar mis datos [Actualización]");
            consola.mostrar(OPCION_CAMBIAR_CONTRASENA + ". Cambiar mi contraseña [Actualización]");
            consola.mostrar(OPCION_CERRAR_SESION + ". Cerrar sesión");
            int opcion = consola.leerOpcion("Seleccione una opción", 0, 3);
            switch (opcion) {
                case OPCION_VER_PERFIL -> verPerfil();
                case OPCION_ACTUALIZAR_DATOS -> actualizarDatos();
                case OPCION_CAMBIAR_CONTRASENA -> cambiarContrasena();
                default -> continuar = false;
            }
        }
        consola.mostrar("Sesión cerrada.");
    }

    private void verPerfil() {
        ejecutarConResultado(() -> usuarioService.consultarPorId(usuarioActual.idUsuario()))
                .ifPresent(usuario -> {
                    usuarioActual = usuario;
                    vistaUsuario.mostrarDetalle(usuario);
                });
    }

    private void actualizarDatos() {
        ejecutarConResultado(() -> usuarioService.actualizarDatos(
                usuarioActual.idUsuario(), vistaUsuario.leerDatosActualizacion(usuarioActual)))
                .ifPresent(usuario -> {
                    usuarioActual = usuario;
                    consola.mostrarExito("Datos actualizados.");
                    vistaUsuario.mostrarDetalle(usuario);
                });
    }

    private void cambiarContrasena() {
        ejecutar(() -> {
            String contrasenaActual = consola.leerContrasena("Contraseña actual");
            String contrasenaNueva = vistaUsuario.leerContrasenaNueva("Contraseña nueva");
            usuarioService.cambiarContrasena(
                    usuarioActual.idUsuario(), contrasenaActual, contrasenaNueva);
            consola.mostrarExito("Contraseña actualizada.");
        });
    }
}
