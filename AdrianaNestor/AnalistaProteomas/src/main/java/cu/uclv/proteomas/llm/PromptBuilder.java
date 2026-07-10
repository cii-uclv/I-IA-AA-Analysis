package cu.uclv.proteomas.llm;

import cu.uclv.proteomas.aaontology.DescriptorProteina;
import cu.uclv.proteomas.model.Secuencia;

public class PromptBuilder {

    public String promptSistema() {
        //Aqui le damos el prompt generico que debe seguir nuestro LLM
        return """
        Eres un bioinformático experto en clasificación enzimática.
        Se te proporcionan descriptores fisicoquímicos calculados a partir de la secuencia de aminoácidos.
        Debes responder ÚNICAMENTE con un objeto JSON válido, sin texto adicional, sin explicaciones,
        sin markdown. El JSON debe tener estas claves exactas:
        {
          "es_enzima": boolean,
          "confianza_enzima": number (0-1),
          "es_hidrolasa": boolean o null,
          "familia_sugerida": "GH18"|"GH19"|"AMP"|"otra" o null,
          "confianza_familia": number (0-1) o null,
          "numero_ec": string en formato N.N.N.N o null,
          "razonamiento": string en español, breve
        }
        Asegúrate de que el JSON sea válido.
        """;
    }

    //Aqui afinamos el prompt con el proteoma actual
    public String promptUsuario(Secuencia secuencia, DescriptorProteina descriptor) {
        StringBuilder sb = new StringBuilder();
        sb.append("Secuencia a clasificar (id_proteoma=").append(secuencia.getIdProteoma())
          .append(", id_secuencia=").append(secuencia.getId()).append("):\n\n");
        sb.append(secuencia.getSecuenciaAa()).append("\n\n");
        sb.append("Descriptores fisicoquimicos (AAontology, 8 categorias):\n");
        sb.append(descriptor.comoContextoPrompt());
        sb.append("\nResponde unicamente con el objeto JSON especificado.");
        sb.append("\n\nRecuerda: responde ÚNICAMENTE con el JSON, sin texto adicional.");
        return sb.toString();
    }
}
