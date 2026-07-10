package cu.uclv.proteomas.aaontology;

/**
 * Las 8 categorias jerarquicas de nivel superior en las que AAontology
 * (Kallies et al., 2023) organiza las 586 escalas de propiedades de
 * aminoacidos. Cada escala original pertenece a exactamente una de estas
 * categorias; aqui se usan como "buckets" para agregar los descriptores
 * fisicoquimicos que se calculan por secuencia antes de pasarlos al LLM.
 */
public enum CategoriaAAontology {
    COMPOSICION("Composition", "Frecuencia y tipo de aminoacidos presentes en la secuencia"),
    AUTOCORRELACION("Autocorrelation", "Patrones de repeticion/periodicidad de propiedades a lo largo de la cadena"),
    ENERGIA("Energetic Properties", "Propiedades termodinamicas: entalpia, entropia, energia libre asociadas a cada residuo"),
    FORMA("Shape", "Volumen, area superficial y geometria de las cadenas laterales"),
    ESTRUCTURA_SECUNDARIA("Structure-Activity / Secondary Structure", "Propension a formar hélices alfa, laminas beta o giros"),
    POLARIDAD("Polarity / Charge", "Hidrofobicidad, carga neta e interacciones electrostaticas"),
    COMPOSICION_ASA("ASA / Exposure", "Area de superficie accesible al solvente, enterramiento/exposicion del residuo"),
    OTRAS("Others", "Escalas misceláneas no cubiertas por las 7 categorias anteriores (p. ej. mutabilidad, flexibilidad)");

    private final String nombreIngles;
    private final String descripcion;

    CategoriaAAontology(String nombreIngles, String descripcion) {
        this.nombreIngles = nombreIngles;
        this.descripcion = descripcion;
    }

    public String getNombreIngles() { return nombreIngles; }
    public String getDescripcion() { return descripcion; }
}
