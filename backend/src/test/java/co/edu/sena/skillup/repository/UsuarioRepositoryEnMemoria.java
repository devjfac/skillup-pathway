package co.edu.sena.skillup.repository;

import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import co.edu.sena.skillup.exception.RegistroDuplicadoException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Repositorio en memoria para probar la lógica de negocio sin una base de datos.
 *
 * <p>Guarda copias de las entidades para que los cambios solo se apliquen al invocar
 * los métodos del repositorio, igual que ocurre con JDBC.</p>
 */
public class UsuarioRepositoryEnMemoria implements UsuarioRepository {

    private final Map<Long, Usuario> usuarios = new LinkedHashMap<>();
    private long siguienteId = 1L;

    @Override
    public Usuario guardar(Usuario usuario) {
        if (existeCorreo(usuario.getCorreoElectronico(), null)) {
            throw new RegistroDuplicadoException("Correo duplicado");
        }
        Usuario copia = copiar(usuario);
        copia.setIdUsuario(siguienteId++);
        copia.setFechaCreacion(LocalDateTime.now());
        usuarios.put(copia.getIdUsuario(), copia);
        return copiar(copia);
    }

    @Override
    public Optional<Usuario> buscarPorId(long idUsuario) {
        return Optional.ofNullable(usuarios.get(idUsuario)).map(UsuarioRepositoryEnMemoria::copiar);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
        return usuarios.values().stream()
                .filter(usuario -> usuario.getCorreoElectronico().equals(correoElectronico))
                .findFirst()
                .map(UsuarioRepositoryEnMemoria::copiar);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarios.values().stream()
                .sorted(Comparator.comparing(Usuario::getIdUsuario))
                .map(UsuarioRepositoryEnMemoria::copiar)
                .toList();
    }

    @Override
    public List<Usuario> buscarPorTexto(String texto) {
        String buscado = texto.toLowerCase(Locale.ROOT);
        return usuarios.values().stream()
                .filter(usuario -> (usuario.getNombreCompleto() + " "
                        + usuario.getCorreoElectronico() + " " + usuario.getNumeroDocumento())
                        .toLowerCase(Locale.ROOT).contains(buscado))
                .map(UsuarioRepositoryEnMemoria::copiar)
                .toList();
    }

    @Override
    public boolean existeCorreo(String correoElectronico, Long idUsuarioExcluido) {
        return usuarios.values().stream()
                .anyMatch(usuario -> usuario.getCorreoElectronico().equals(correoElectronico)
                        && !Objects.equals(usuario.getIdUsuario(), idUsuarioExcluido));
    }

    @Override
    public boolean existeDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento, Long idUsuarioExcluido) {
        return usuarios.values().stream()
                .anyMatch(usuario -> usuario.getTipoDocumento() == tipoDocumento
                        && usuario.getNumeroDocumento().equals(numeroDocumento)
                        && !Objects.equals(usuario.getIdUsuario(), idUsuarioExcluido));
    }

    @Override
    public long contarActivosPorRol(NombreRol rol) {
        return usuarios.values().stream()
                .filter(usuario -> usuario.isActivo() && usuario.tieneRol(rol))
                .count();
    }

    @Override
    public boolean actualizarDatos(Usuario usuario) {
        Usuario almacenado = usuarios.get(usuario.getIdUsuario());
        if (almacenado == null) {
            return false;
        }
        almacenado.setTipoDocumento(usuario.getTipoDocumento());
        almacenado.setNumeroDocumento(usuario.getNumeroDocumento());
        almacenado.setNombres(usuario.getNombres());
        almacenado.setApellidos(usuario.getApellidos());
        almacenado.setCorreoElectronico(usuario.getCorreoElectronico());
        return true;
    }

    @Override
    public boolean actualizarContrasena(long idUsuario, String contrasenaHash) {
        Usuario almacenado = usuarios.get(idUsuario);
        if (almacenado == null) {
            return false;
        }
        almacenado.setContrasenaHash(contrasenaHash);
        return true;
    }

    @Override
    public boolean actualizarEstado(long idUsuario, boolean activo) {
        Usuario almacenado = usuarios.get(idUsuario);
        if (almacenado == null) {
            return false;
        }
        almacenado.setActivo(activo);
        return true;
    }

    @Override
    public boolean actualizarControlAcceso(
            long idUsuario, int intentosFallidos, LocalDateTime bloqueadoHasta) {
        Usuario almacenado = usuarios.get(idUsuario);
        if (almacenado == null) {
            return false;
        }
        almacenado.setIntentosFallidos(intentosFallidos);
        almacenado.setBloqueadoHasta(bloqueadoHasta);
        return true;
    }

    @Override
    public boolean asignarRol(long idUsuario, NombreRol rol) {
        Usuario almacenado = usuarios.get(idUsuario);
        if (almacenado == null || almacenado.tieneRol(rol)) {
            return false;
        }
        almacenado.agregarRol(rol);
        return true;
    }

    @Override
    public boolean quitarRol(long idUsuario, NombreRol rol) {
        Usuario almacenado = usuarios.get(idUsuario);
        if (almacenado == null || !almacenado.tieneRol(rol)) {
            return false;
        }
        almacenado.quitarRol(rol);
        return true;
    }

    @Override
    public boolean eliminar(long idUsuario) {
        return usuarios.remove(idUsuario) != null;
    }

    private static Usuario copiar(Usuario origen) {
        Usuario copia = new Usuario();
        copia.setIdUsuario(origen.getIdUsuario());
        copia.setTipoDocumento(origen.getTipoDocumento());
        copia.setNumeroDocumento(origen.getNumeroDocumento());
        copia.setNombres(origen.getNombres());
        copia.setApellidos(origen.getApellidos());
        copia.setCorreoElectronico(origen.getCorreoElectronico());
        copia.setContrasenaHash(origen.getContrasenaHash());
        copia.setActivo(origen.isActivo());
        copia.setIntentosFallidos(origen.getIntentosFallidos());
        copia.setBloqueadoHasta(origen.getBloqueadoHasta());
        copia.setFechaCreacion(origen.getFechaCreacion());
        copia.setFechaActualizacion(origen.getFechaActualizacion());
        origen.getRoles().forEach(copia::agregarRol);
        return copia;
    }
}
