package co.edu.sena.skillup.entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Cuenta de usuario de SkillUp Pathway, mapeada a la tabla {@code usuario}.
 *
 * <p>Los roles provienen de la tabla asociativa {@code usuario_rol}. La contraseña se
 * guarda únicamente como hash; nunca en texto plano.</p>
 */
public class Usuario {

    private Long idUsuario;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String correoElectronico;
    private String contrasenaHash;
    private boolean activo = true;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private final Set<NombreRol> roles = EnumSet.noneOf(NombreRol.class);

    /**
     * Crea un usuario vacío; los datos se asignan con los métodos de acceso.
     */
    public Usuario() {
        // Constructor requerido para el mapeo desde ResultSet.
    }

    /**
     * Asigna un rol al usuario.
     *
     * @param rol rol que se agrega
     */
    public void agregarRol(NombreRol rol) {
        roles.add(Objects.requireNonNull(rol, "El rol es obligatorio"));
    }

    /**
     * Retira un rol del usuario.
     *
     * @param rol rol que se retira
     */
    public void quitarRol(NombreRol rol) {
        roles.remove(rol);
    }

    /**
     * Indica si el usuario tiene asignado el rol recibido.
     *
     * @param rol rol que se consulta
     * @return {@code true} si el usuario tiene el rol
     */
    public boolean tieneRol(NombreRol rol) {
        return roles.contains(rol);
    }

    /**
     * Indica si la cuenta está bloqueada temporalmente en el momento recibido (HU-02, CA-03).
     *
     * @param momento fecha y hora de referencia
     * @return {@code true} si el bloqueo sigue vigente
     */
    public boolean estaBloqueado(LocalDateTime momento) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(momento);
    }

    /**
     * Devuelve los nombres y apellidos separados por un espacio.
     *
     * @return nombre completo del usuario
     */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Set<NombreRol> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Usuario usuario) || idUsuario == null) {
            return false;
        }
        return idUsuario.equals(usuario.idUsuario);
    }

    @Override
    public int hashCode() {
        return idUsuario == null ? System.identityHashCode(this) : idUsuario.hashCode();
    }

    @Override
    public String toString() {
        return "Usuario[idUsuario=" + idUsuario + ", correoElectronico=" + correoElectronico
                + ", roles=" + roles + ", activo=" + activo + "]";
    }
}
