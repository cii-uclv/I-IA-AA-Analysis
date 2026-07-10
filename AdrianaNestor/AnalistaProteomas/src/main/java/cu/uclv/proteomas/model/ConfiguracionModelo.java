package cu.uclv.proteomas.model;

public class ConfiguracionModelo {
    private Integer idModelo;
    private String nombre;
    private String descripcion;
    private String metadatosJson; // columna JSONB, se maneja como texto JSON crudo en la app

    public Integer getIdModelo() { return idModelo; }
    public void setIdModelo(Integer idModelo) { this.idModelo = idModelo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMetadatosJson() { return metadatosJson; }
    public void setMetadatosJson(String metadatosJson) { this.metadatosJson = metadatosJson; }

    @Override
    public String toString() { return nombre; }
}
