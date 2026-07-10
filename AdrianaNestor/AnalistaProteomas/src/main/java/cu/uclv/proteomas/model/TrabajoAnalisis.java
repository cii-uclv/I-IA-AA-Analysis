package cu.uclv.proteomas.model;

import java.time.LocalDateTime;

public class TrabajoAnalisis {
    private Integer idTrabajo;
    private Enums.TipoAnalisis tipo;
    private Enums.EstadoAnalisis estado = Enums.EstadoAnalisis.en_cola;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String mensajeError;
    private Integer idProteoma;
    private Integer idUsuario;
    private Integer idModelo;

    public Integer getIdTrabajo() { return idTrabajo; }
    public void setIdTrabajo(Integer idTrabajo) { this.idTrabajo = idTrabajo; }

    public Enums.TipoAnalisis getTipo() { return tipo; }
    public void setTipo(Enums.TipoAnalisis tipo) { this.tipo = tipo; }

    public Enums.EstadoAnalisis getEstado() { return estado; }
    public void setEstado(Enums.EstadoAnalisis estado) { this.estado = estado; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public Integer getIdProteoma() { return idProteoma; }
    public void setIdProteoma(Integer idProteoma) { this.idProteoma = idProteoma; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdModelo() { return idModelo; }
    public void setIdModelo(Integer idModelo) { this.idModelo = idModelo; }

    @Override
    public String toString() {
        return "Trabajo#" + idTrabajo + " proteoma=" + idProteoma + " [" + estado + "]";
    }
}
