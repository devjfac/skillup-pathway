package co.edu.sena.skillup.entity;

/**
 * Tipos de documento de identidad admitidos (columna {@code usuario.tipo_documento}).
 */
public enum TipoDocumento {
    CC("Cédula de ciudadanía"),
    CE("Cédula de extranjería"),
    TI("Tarjeta de identidad"),
    PASAPORTE("Pasaporte");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
