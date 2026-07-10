package cu.uclv.proteomas.model;

import java.time.LocalDateTime;

/**
 * Resultado de clasificacion jerarquica de 3 niveles, enriquecido con el
 * razonamiento en lenguaje natural generado por el LLM (en nuestro caso es un modelo hosteado en Hugging Face).
 */
public class Respuesta {
    private Integer idTrabajo;
    private Integer idProteoma;
    private Long idSecuencia;

    // Nivel 1
    private boolean esEnzima;
    private float confianzaEnzima;

    // Nivel 2
    private Boolean esHidrolasa;

    // Nivel 3
    private Enums.Familia familiaSugerida;
    private Float confianzaFamilia;
    private String numeroEc; // formato N.N.N.N, validado tambien por dominio ec_number en la BD

    private String razonamiento;
    private LocalDateTime fecha;

    public Integer getIdTrabajo() { return idTrabajo; }
    public void setIdTrabajo(Integer idTrabajo) { this.idTrabajo = idTrabajo; }

    public Integer getIdProteoma() { return idProteoma; }
    public void setIdProteoma(Integer idProteoma) { this.idProteoma = idProteoma; }

    public Long getIdSecuencia() { return idSecuencia; }
    public void setIdSecuencia(Long idSecuencia) { this.idSecuencia = idSecuencia; }

    public boolean isEsEnzima() { return esEnzima; }
    public void setEsEnzima(boolean esEnzima) { this.esEnzima = esEnzima; }

    public float getConfianzaEnzima() { return confianzaEnzima; }
    public void setConfianzaEnzima(float confianzaEnzima) { this.confianzaEnzima = confianzaEnzima; }

    public Boolean getEsHidrolasa() { return esHidrolasa; }
    public void setEsHidrolasa(Boolean esHidrolasa) { this.esHidrolasa = esHidrolasa; }

    public Enums.Familia getFamiliaSugerida() { return familiaSugerida; }
    public void setFamiliaSugerida(Enums.Familia familiaSugerida) { this.familiaSugerida = familiaSugerida; }

    public Float getConfianzaFamilia() { return confianzaFamilia; }
    public void setConfianzaFamilia(Float confianzaFamilia) { this.confianzaFamilia = confianzaFamilia; }

    public String getNumeroEc() { return numeroEc; }
    public void setNumeroEc(String numeroEc) { this.numeroEc = numeroEc; }

    public String getRazonamiento() { return razonamiento; }
    public void setRazonamiento(String razonamiento) { this.razonamiento = razonamiento; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
