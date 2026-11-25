package co.edu.sena.skillup.entity;

/**
 * Roles de la plataforma; cada valor coincide con un registro de la tabla {@code rol}.
 */
public enum NombreRol {
    ESTUDIANTE("Estudiante"),
    INSTRUCTOR("Instructor"),
    MENTOR("Mentor"),
    EMPRESA("Empresa"),
    ADMINISTRADOR("Administrador");

    private final String etiqueta;

    NombreRol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
