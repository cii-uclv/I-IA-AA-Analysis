package cu.uclv.proteomas.aaontology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de escalas fisicoquimicas usadas por el DescriptorCalculator.
 *
 * NOTA DE ALCANCE: AAontology organiza 586 escalas en 8 categorias. Reproducir
 * las 586 tablas completas escapa al alcance de este proyecto de referencia;
 * aqui se incluye UNA escala representativa (publicada, de uso comun en
 * bioinformatica) por cada una de las 8 categorias, de forma que la
 * arquitectura (categoria -> lista de escalas -> descriptor agregado) sea
 * identica a la que tendria el sistema completo. Anadir mas escalas es tan
 * simple como agregar mas entradas de tipo Escala.
 */
public final class EscalasRegistry {

    private static final List<Escala> REGISTRO = List.of(

        // POLARIDAD: hidrofobicidad de Kyte & Doolittle (1982)
        new Escala("KD82_HIDROFOBICIDAD", "Kyte-Doolittle Hydrophobicity", CategoriaAAontology.POLARIDAD, mapa(
            'A', 1.8, 'R', -4.5, 'N', -3.5, 'D', -3.5, 'C', 2.5, 'Q', -3.5, 'E', -3.5, 'G', -0.4,
            'H', -3.2, 'I', 4.5, 'L', 3.8, 'K', -3.9, 'M', 1.9, 'F', 2.8, 'P', -1.6, 'S', -0.8,
            'T', -0.7, 'W', -0.9, 'Y', -1.3, 'V', 4.2
        )),

        // FORMA: volumen/bulkiness de Zimmerman et al. (1968), en angstrom^3
        new Escala("ZIM68_VOLUMEN", "Zimmerman Bulkiness/Volume", CategoriaAAontology.FORMA, mapa(
            'A', 52.6, 'R', 109.1, 'N', 75.7, 'D', 68.4, 'C', 68.3, 'Q', 89.7, 'E', 84.7, 'G', 36.3,
            'H', 91.9, 'I', 102.0, 'L', 102.0, 'K', 105.1, 'M', 97.7, 'F', 113.9, 'P', 73.6, 'S', 54.9,
            'T', 71.2, 'W', 135.4, 'Y', 116.2, 'V', 85.1
        )),

        // ESTRUCTURA SECUNDARIA: propension a helice alfa, Chou & Fasman (1978)
        new Escala("CF78_HELICE_ALFA", "Chou-Fasman Alpha-Helix Propensity", CategoriaAAontology.ESTRUCTURA_SECUNDARIA, mapa(
            'A', 1.42, 'R', 0.98, 'N', 0.67, 'D', 1.01, 'C', 0.70, 'Q', 1.11, 'E', 1.51, 'G', 0.57,
            'H', 1.00, 'I', 1.08, 'L', 1.21, 'K', 1.16, 'M', 1.45, 'F', 1.13, 'P', 0.57, 'S', 0.77,
            'T', 0.83, 'W', 1.08, 'Y', 0.69, 'V', 1.06
        )),

        // ENERGIA: energia libre de transferencia agua->octanol, Fauchere & Pliska (1983), kcal/mol
        new Escala("FP83_ENERGIA_TRANSFERENCIA", "Fauchere-Pliska Transfer Free Energy", CategoriaAAontology.ENERGIA, mapa(
            'A', 0.31, 'R', -1.01, 'N', -0.60, 'D', -0.77, 'C', 1.54, 'Q', -0.22, 'E', -0.64, 'G', 0.00,
            'H', 0.13, 'I', 1.80, 'L', 1.70, 'K', -0.99, 'M', 1.23, 'F', 1.79, 'P', 0.72, 'S', -0.04,
            'T', 0.26, 'W', 2.25, 'Y', 0.96, 'V', 1.22
        )),

        // ASA / EXPOSICION: area promedio enterrada, Rose et al. (1985), fraccion 0-1
        new Escala("ROSE85_AREA_ENTERRADA", "Rose Average Area Buried", CategoriaAAontology.COMPOSICION_ASA, mapa(
            'A', 0.74, 'R', 0.64, 'N', 0.63, 'D', 0.62, 'C', 0.91, 'Q', 0.62, 'E', 0.62, 'G', 0.72,
            'H', 0.78, 'I', 0.88, 'L', 0.85, 'K', 0.52, 'M', 0.85, 'F', 0.88, 'P', 0.64, 'S', 0.66,
            'T', 0.70, 'W', 0.85, 'Y', 0.76, 'V', 0.86
        )),

        // OTRAS: indice de mutabilidad relativa, Dayhoff (1978)
        new Escala("DAY78_MUTABILIDAD", "Dayhoff Relative Mutability", CategoriaAAontology.OTRAS, mapa(
            'A', 100.0, 'R', 65.0, 'N', 134.0, 'D', 106.0, 'C', 20.0, 'Q', 93.0, 'E', 102.0, 'G', 49.0,
            'H', 66.0, 'I', 96.0, 'L', 40.0, 'K', 56.0, 'M', 94.0, 'F', 41.0, 'P', 56.0, 'S', 120.0,
            'T', 97.0, 'W', 18.0, 'Y', 41.0, 'V', 74.0
        ))
    );

    /** COMPOSICION y AUTOCORRELACION se derivan directamente de la secuencia (ver DescriptorCalculator), no de una escala fija. */
    public static List<Escala> todas() {
        return REGISTRO;
    }

    public static List<Escala> porCategoria(CategoriaAAontology categoria) {
        return REGISTRO.stream().filter(e -> e.getCategoria() == categoria).toList();
    }

    /** Escala de hidrofobicidad usada como base para el calculo de autocorrelacion. */
    public static Escala hidrofobicidadBase() {
        return REGISTRO.get(0);
    }

    private static Map<Character, Double> mapa(Object... pares) {
        Map<Character, Double> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((Character) pares[i], (Double) pares[i + 1]);
        }
        return m;
    }

    private EscalasRegistry() {}
}
