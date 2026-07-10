package cu.uclv.proteomas.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Respuesta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el JSON devuelto por HuggingFace en un objeto Respuesta parcialmente
 * poblado (faltan id_trabajo/id_proteoma/id_secuencia, que asigna el orquestador).
 * Es tolerante a que el modelo envuelva el JSON en texto o backticks, aunque
 * el prompt se lo prohiba explicitamente.
 */
public class ClasificacionParser {

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
    private static final Pattern EC_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");

    public Respuesta parsear(String textoLlm) {
        //Convertimos la respuesta del LLM en una respuesta valida para nuestra base de datos
        Matcher m = JSON_PATTERN.matcher(textoLlm);
        if (!m.find()) {
            throw new IllegalArgumentException("La respuesta del LLM no contiene un JSON valido: " + textoLlm);
        }
        JsonObject obj = JsonParser.parseString(m.group()).getAsJsonObject();

        Respuesta r = new Respuesta();
        r.setEsEnzima(obj.get("es_enzima").getAsBoolean());
        r.setConfianzaEnzima(clamp01(obj.get("confianza_enzima").getAsFloat()));

        if (obj.has("es_hidrolasa") && !obj.get("es_hidrolasa").isJsonNull()) {
            r.setEsHidrolasa(obj.get("es_hidrolasa").getAsBoolean());
        }

        if (obj.has("familia_sugerida") && !obj.get("familia_sugerida").isJsonNull()) {
            try {
                r.setFamiliaSugerida(Enums.Familia.valueOf(obj.get("familia_sugerida").getAsString()));
            } catch (IllegalArgumentException ignored) {
                r.setFamiliaSugerida(Enums.Familia.otra);
            }
        }

        if (obj.has("confianza_familia") && !obj.get("confianza_familia").isJsonNull()) {
            r.setConfianzaFamilia(clamp01(obj.get("confianza_familia").getAsFloat()));
        }

        if (obj.has("numero_ec") && !obj.get("numero_ec").isJsonNull()) {
            String ec = obj.get("numero_ec").getAsString().trim();
            // El dominio ec_number y el CHECK chk_ec_formato de la BD exigen este formato;
            // si el LLM devuelve algo mal formado, se descarta en vez de romper el INSERT.
            r.setNumeroEc(EC_PATTERN.matcher(ec).matches() ? ec : null);
        }

        r.setRazonamiento(obj.has("razonamiento") ? obj.get("razonamiento").getAsString() : null);
        return r;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
