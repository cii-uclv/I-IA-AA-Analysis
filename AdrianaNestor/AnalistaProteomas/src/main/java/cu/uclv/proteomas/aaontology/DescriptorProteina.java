package cu.uclv.proteomas.aaontology;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resultado del calculo de descriptores fisicoquimicos de una secuencia,
 * organizado por las 8 categorias jerarquicas de AAontology. Este objeto es
 * el que se serializa como contexto dentro del prompt enviado
 */
public class DescriptorProteina {

    private final long idSecuencia;
    private final int longitud;
    private final double pesoMolecularKDa;
    private final double cargaNetaPh7;
    private final double fraccionAromatica;
    private final Map<Character, Double> composicionPorcentual = new TreeMap<>();
    private final Map<CategoriaAAontology, Double> promedioPorCategoria = new EnumMap<>(CategoriaAAontology.class);
    private final double autocorrelacionHidrofobicidadLag1;

    public DescriptorProteina(long idSecuencia, int longitud, double pesoMolecularKDa, double cargaNetaPh7,
                               double fraccionAromatica, double autocorrelacionHidrofobicidadLag1) {
        this.idSecuencia = idSecuencia;
        this.longitud = longitud;
        this.pesoMolecularKDa = pesoMolecularKDa;
        this.cargaNetaPh7 = cargaNetaPh7;
        this.fraccionAromatica = fraccionAromatica;
        this.autocorrelacionHidrofobicidadLag1 = autocorrelacionHidrofobicidadLag1;
    }

    public void setComposicion(char aa, double porcentaje) { composicionPorcentual.put(aa, porcentaje); }
    public void setPromedioCategoria(CategoriaAAontology cat, double valor) { promedioPorCategoria.put(cat, valor); }

    public long getIdSecuencia() { return idSecuencia; }
    public int getLongitud() { return longitud; }
    public double getPesoMolecularKDa() { return pesoMolecularKDa; }
    public double getCargaNetaPh7() { return cargaNetaPh7; }
    public double getFraccionAromatica() { return fraccionAromatica; }
    public double getAutocorrelacionHidrofobicidadLag1() { return autocorrelacionHidrofobicidadLag1; }
    public Map<CategoriaAAontology, Double> getPromedioPorCategoria() { return promedioPorCategoria; }
    public Map<Character, Double> getComposicionPorcentual() { return composicionPorcentual; }

    /**
     * Formatea los descriptores como un bloque de texto estructurado que se
     * inserta en el prompt del LLM, agrupado por las 8 categorias de AAontology.
     */
    public String comoContextoPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- Longitud: %d aminoacidos%n", longitud));
        sb.append(String.format("- Peso molecular estimado: %.2f kDa%n", pesoMolecularKDa));
        sb.append(String.format("- Carga neta estimada a pH 7: %.2f%n", cargaNetaPh7));
        sb.append(String.format("- Fraccion de residuos aromaticos (F,W,Y): %.1f%%%n", fraccionAromatica * 100));
        sb.append("\nDescriptores agregados por categoria AAontology:\n");
        for (CategoriaAAontology cat : CategoriaAAontology.values()) {
            switch (cat) {
                case COMPOSICION -> sb.append(String.format(
                    "  [%s] Composicion de aminoacidos: %s%n", cat.getNombreIngles(), formateaComposicionTop()));
                case AUTOCORRELACION -> sb.append(String.format(
                    "  [%s] Autocorrelacion de hidrofobicidad (lag=1): %.3f%n", cat.getNombreIngles(), autocorrelacionHidrofobicidadLag1));
                default -> {
                    Double v = promedioPorCategoria.get(cat);
                    if (v != null) {
                        sb.append(String.format("  [%s] Valor promedio de escala representativa: %.3f%n", cat.getNombreIngles(), v));
                    }
                }
            }
        }
        return sb.toString();
    }

    private String formateaComposicionTop() {
        return composicionPorcentual.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> e.getKey() + ":" + String.format("%.1f%%", e.getValue()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("n/d");
    }
}
