package cu.uclv.proteomas.aaontology;

import java.util.HashMap;
import java.util.Map;

/**
 * Calcula, a partir de una secuencia de aminoacidos, un DescriptorProteina
 * que agrega valores fisicoquimicos por cada una de las 8 categorias
 * jerarquicas de AAontology. Este es el "contexto estructural" que luego
 * se inyecta en el prompt enviado al LLM.
 */
public class DescriptorCalculator {

    // Pesos monoisotopicos promedio por residuo (Da), tabla estandar.
    private static final Map<Character, Double> PESO_RESIDUO = Map.ofEntries(
        Map.entry('A', 71.08), Map.entry('R', 156.19), Map.entry('N', 114.10), Map.entry('D', 115.09),
        Map.entry('C', 103.14), Map.entry('Q', 128.13), Map.entry('E', 129.12), Map.entry('G', 57.05),
        Map.entry('H', 137.14), Map.entry('I', 113.16), Map.entry('L', 113.16), Map.entry('K', 128.17),
        Map.entry('M', 131.19), Map.entry('F', 147.18), Map.entry('P', 97.12), Map.entry('S', 87.08),
        Map.entry('T', 101.10), Map.entry('W', 186.21), Map.entry('Y', 163.18), Map.entry('V', 99.13)
    );
    private static final double AGUA = 18.02;

    public DescriptorProteina calcular(long idSecuencia, String secuenciaAaCruda) {
        String seq = secuenciaAaCruda == null ? "" : secuenciaAaCruda.toUpperCase().replaceAll("[^A-Z]", "");
        int n = seq.length();

        // --- Composicion ---
        Map<Character, Integer> conteo = new HashMap<>();
        for (char c : seq.toCharArray()) conteo.merge(c, 1, Integer::sum);

        // --- Peso molecular ---
        double pesoDa = AGUA;
        for (char c : seq.toCharArray()) pesoDa += PESO_RESIDUO.getOrDefault(c, 110.0);
        double pesoKDa = pesoDa / 1000.0;

        // --- Carga neta a pH 7 (aproximacion simple, no un pKa completo) ---
        int positivos = conteo.getOrDefault('K', 0) + conteo.getOrDefault('R', 0);
        double histidinaParcial = conteo.getOrDefault('H', 0) * 0.1; // H esta ~10% protonada a pH7
        int negativos = conteo.getOrDefault('D', 0) + conteo.getOrDefault('E', 0);
        double cargaNeta = (positivos + histidinaParcial) - negativos;

        // --- Fraccion aromatica ---
        int aromaticos = conteo.getOrDefault('F', 0) + conteo.getOrDefault('W', 0) + conteo.getOrDefault('Y', 0);
        double fraccionAromatica = n == 0 ? 0.0 : (double) aromaticos / n;

        // --- Autocorrelación centrada y normalizada (similar a Pearson, lag=1) ---
        Escala hidrofobicidad = EscalasRegistry.hidrofobicidadBase();
        double autocorrelacion = autocorrelacionLag1(seq, hidrofobicidad);

        DescriptorProteina d = new DescriptorProteina(idSecuencia, n, pesoKDa, cargaNeta, fraccionAromatica, autocorrelacion);

        for (Map.Entry<Character, Integer> e : conteo.entrySet()) {
            d.setComposicion(e.getKey(), n == 0 ? 0.0 : 100.0 * e.getValue() / n);
        }

        // --- Promedio por escala representativa de cada categoria restante ---
        for (Escala escala : EscalasRegistry.todas()) {
            if (escala.getCategoria() == CategoriaAAontology.POLARIDAD
                    || escala.getCategoria() == CategoriaAAontology.FORMA
                    || escala.getCategoria() == CategoriaAAontology.ESTRUCTURA_SECUNDARIA
                    || escala.getCategoria() == CategoriaAAontology.ENERGIA
                    || escala.getCategoria() == CategoriaAAontology.COMPOSICION_ASA
                    || escala.getCategoria() == CategoriaAAontology.OTRAS) {
                d.setPromedioCategoria(escala.getCategoria(), promedio(seq, escala));
            }
        }

        return d;
    }

    private double promedio(String seq, Escala escala) {
        if (seq.isEmpty()) return 0.0;
        double suma = 0.0;
        for (char c : seq.toCharArray()) suma += escala.valorDe(c);
        return suma / seq.length();
    }

    private double autocorrelacionLag1(String seq, Escala escala) {
        int n = seq.length();
        if (n < 2) return 0.0;
        double media = promedio(seq, escala);
        double numerador = 0.0;
        double denominador = 0.0;
        for (int i = 0; i < n - 1; i++) {
            double vi = escala.valorDe(seq.charAt(i)) - media;
            double vi1 = escala.valorDe(seq.charAt(i + 1)) - media;
            numerador += vi * vi1;
        }
        for (int i = 0; i < n; i++) {
            double vi = escala.valorDe(seq.charAt(i)) - media;
            denominador += vi * vi;
        }
        return denominador == 0.0 ? 0.0 : numerador / denominador;
    }
}
