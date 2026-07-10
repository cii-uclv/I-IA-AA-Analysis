package cu.uclv.proteomas.model;

import java.time.LocalDateTime;

/**
 * Supertipo USUARIO. La herencia exclusiva/total con Investigador y
 * Administrador se modela en Java con dos subclases; cual subtabla
 * existe realmente en la BD la decide fn_validar_tipo_investigador /
 * fn_validar_tipo_administrador, no la aplicacion.
 */
public class Usuario {
    private Integer idUsuario;
    private String nombreUsuario;
    private String correo;
    private String hashContrasena;
    private String institucion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
    private boolean activo = true;
    private Enums.TipoUsuario tipoUsuario;

    public Usuario() {}

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getHashContrasena() { return hashContrasena; }
    public void setHashContrasena(String hashContrasena) { this.hashContrasena = hashContrasena; }

    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Enums.TipoUsuario getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(Enums.TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }


    @Override
    public String toString() {
        return nombreUsuario + " (" + tipoUsuario + ")";
    }
}
