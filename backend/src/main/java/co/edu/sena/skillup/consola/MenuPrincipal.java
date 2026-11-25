package co.edu.sena.skillup.consola;

import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.service.UsuarioService;

/**
 * Menú inicial: registro de estudiantes (RF-01) e inicio de sesión (RF-02).
 */
public class MenuPrincipal extends MenuBase {

    private static final int OPCION_SALIR = 0;
    private static final int OPCION_REGISTRARSE = 1;
    private static final int OPCION_INICIAR_SESION = 2;

    /**
     * Crea el menú principal.
     *
     * @param consola entrada y salida de texto
     * @param usuarioService lógica de negocio del módulo de usuarios
     */
    public MenuPrincipal(Consola consola, UsuarioService usuarioService) {
        super(consola, usuarioService);
    }

    /**
     * Muestra el menú hasta que el usuario elija salir.
     */
    public void iniciar() {
        configurarAdministradorInicial();
        boolean continuar = true;
        while (continuar) {
            consola.mostrarTitulo("SkillUp Pathway - Módulo de gestión de usuarios");
            consola.mostrar(OPCION_REGISTRARSE + ". Registrarme como estudiante (RF-01)");
            consola.mostrar(OPCION_INICIAR_SESION + ". Iniciar sesión (RF-02)");
            consola.mostrar(OPCION_SALIR + ". Salir");
            int opcion = consola.leerOpcion("Seleccione una opción", 0, 2);
            switch (opcion) {
                case OPCION_REGISTRARSE -> registrarEstudiante();
                case OPCION_INICIAR_SESION -> iniciarSesion();
                default -> continuar = false;
            }
        }
        consola.mostrar("Hasta pronto.");
    }

    private void configurarAdministradorInicial() {
        boolean existeAdministrador = ejecutarConResultado(
                usuarioService::existeAdministradorActivo).orElse(Boolean.TRUE);
        while (!existeAdministrador) {
            consola.mostrarTitulo("Configuración inicial: cuenta de administrador");
            consola.mostrar("La base de datos no tiene un administrador activo."
                    + " Registre la primera cuenta.");
            existeAdministrador = ejecutarConResultado(() -> {
                RegistroUsuarioDTO datos = vistaUsuario.leerDatosRegistro();
                return usuarioService.registrarAdministradorInicial(datos);
            }).map(administrador -> {
                consola.mostrarExito("Administrador creado con el identificador "
                        + administrador.idUsuario() + ".");
                return Boolean.TRUE;
            }).orElse(Boolean.FALSE);
        }
    }

    private void registrarEstudiante() {
        consola.mostrarTitulo("Registro de estudiante (RF-01) [Inserción]");
        ejecutarConResultado(() -> usuarioService.registrarEstudiante(
                vistaUsuario.leerDatosRegistro()))
                .ifPresent(usuario -> {
                    consola.mostrarExito("Cuenta creada. Ya puede iniciar sesión.");
                    vistaUsuario.mostrarDetalle(usuario);
                });
    }

    private void iniciarSesion() {
        consola.mostrarTitulo("Inicio de sesión (RF-02) [Consulta]");
        ejecutarConResultado(() -> {
            String correoElectronico = consola.leerTexto("Correo electrónico");
            String contrasena = consola.leerContrasena("Contraseña");
            return usuarioService.iniciarSesion(correoElectronico, contrasena);
        }).ifPresent(this::abrirMenuSegunRol);
    }

    private void abrirMenuSegunRol(UsuarioDTO usuario) {
        consola.mostrarExito("Bienvenido(a), " + usuario.nombreCompleto() + ".");
        if (usuario.esAdministrador()) {
            new MenuAdministrador(consola, usuarioService, usuario).iniciar();
        } else {
            new MenuPerfil(consola, usuarioService, usuario).iniciar();
        }
    }
}
