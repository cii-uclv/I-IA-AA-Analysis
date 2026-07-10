package cu.uclv.proteomas.model;

import java.time.LocalDateTime;

public class Proteoma {
    private Integer idProteoma;
    private String nombreProyecto;
    private String especie;
    private String cepa;
    private String rutaArchivo;
    private String md5Fasta;
    private LocalDateTime fechaSubida;
    private Enums.EstadoProteoma estado = Enums.EstadoProteoma.pendiente;
    private int totalSecuencias; // mantenido por trigger, solo lectura desde la app
    private boolean visibleOtros;
    private Integer idUsuarioPublica;

    public Integer getIdProteoma() { return idProteoma; }
    public void setIdProteoma(Integer idProteoma) { this.idProteoma = idProteoma; }

    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getCepa() { return cepa; }
    public void setCepa(String cepa) { this.cepa = cepa; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public String getMd5Fasta() { return md5Fasta; }
    public void setMd5Fasta(String md5Fasta) { this.md5Fasta = md5Fasta; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public Enums.EstadoProteoma getEstado() { return estado; }
    public void setEstado(Enums.EstadoProteoma estado) { this.estado = estado; }

    public int getTotalSecuencias() { return totalSecuencias; }
    public void setTotalSecuencias(int totalSecuencias) { this.totalSecuencias = totalSecuencias; }

    public boolean isVisibleOtros() { return visibleOtros; }
    public void setVisibleOtros(boolean visibleOtros) { this.visibleOtros = visibleOtros; }

    public Integer getIdUsuarioPublica() { return idUsuarioPublica; }
    public void setIdUsuarioPublica(Integer idUsuarioPublica) { this.idUsuarioPublica = idUsuarioPublica; }

    @Override
    public String toString() {
        return "#" + idProteoma + " " + nombreProyecto + " (" + especie + ") [" + estado + "]";
    }
}
