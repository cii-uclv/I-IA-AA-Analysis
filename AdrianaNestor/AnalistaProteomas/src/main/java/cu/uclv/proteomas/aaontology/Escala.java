package cu.uclv.proteomas.aaontology;

import java.util.Map;

/**
 * Una escala fisicoquimica individual (una de las 586 de AAontology),
 * definida como un mapa aminoacido -> valor numerico, y asociada a una
 * de las 8 categorias jerarquicas.
 */
public class Escala {
    private final String id;
    private final String nombre;
    private final CategoriaAAontology categoria;
    private final Map<Character, Double> valores;

    public Escala(String id, String nombre, CategoriaAAontology categoria, Map<Character, Double> valores) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.valores = valores;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public CategoriaAAontology getCategoria() { return categoria; }

    /** Valor de la escala para un residuo; 0.0 si el caracter no es un aminoacido estandar. */
    public double valorDe(char residuo) {
        return valores.getOrDefault(Character.toUpperCase(residuo), 0.0);
    }
}
