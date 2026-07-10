package cu.uclv.proteomas.model;

/**
 * Entidad debil por identificacion: su PK es (id_proteoma, id).
 * "longitud" la calcula el trigger trg_calcular_longitud en la BD,
 * por lo que la app nunca la envia explicitamente en INSERT/UPDATE.
 */
public class Secuencia {
    private Integer idProteoma;
    private Long id;
    private Integer longitud;
    private String secuenciaAa;

    public Integer getIdProteoma() { return idProteoma; }
    public void setIdProteoma(Integer idProteoma) { this.idProteoma = idProteoma; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getLongitud() { return longitud; }
    public void setLongitud(Integer longitud) { this.longitud = longitud; }

    public String getSecuenciaAa() { return secuenciaAa; }
    public void setSecuenciaAa(String secuenciaAa) { this.secuenciaAa = secuenciaAa; }

    @Override
    public String toString() {
        return "Secuencia[" + idProteoma + "," + id + "] len=" + longitud;
    }
}
