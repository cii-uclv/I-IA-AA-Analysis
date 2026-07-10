package cu.uclv.proteomas.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Esta clase nos permite configurar directamente la ruta, usuario y contraseña de nuestra
 * base de datos, así como la información sobre el modelo de IA que estamos usando y nuestra
 * llave de la API, de tal manera que no queden expuestos.
 */
public final class AppConfig {

    /**
     * Si no coinciden los valores con los que estan en nuestro archivo .env se ignoran.
     */
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

    /**
     * @param key El parametro a buscar en nuestro .env
     * @param defaultValue El valor por defecto que va a tomar nuestro resultado en caso de que haya problemas con la ubicacion o el contenido de la key
     * @return El valor correspondiente a la key dentro de nuestro .env, si no se encuentra se devuelve el defaultValue
     */
    private static String get(String key, String defaultValue) {
        String v = dotenv.get(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key);
        }
        if (v == null || v.isBlank()) {
            v = defaultValue;
        }
        return v;
    }

    public static String dbUrl() {
        return get("DB_URL", "jdbc:postgresql://localhost:5432/proteomas_bd");
    }

    public static String dbUser() {
        return get("DB_USER", "postgres");
    }

    public static String dbPassword() {
        return get("DB_PASSWORD", "");
    }

    public static String hfApiKey() {
        String key = get("HF_API_KEY", null);
        if (key == null) {
            throw new IllegalStateException(
                    "HF_API_KEY no configurada. Define la variable en .env o en el sistema.");
        }
        return key;
    }

    public static String hfApiUrl() {
        return get("HF_API_URL", "https://router.huggingface.co/v1/chat/completions");
    }

    public static String hfModel() {
        return get("HF_MODEL", "meta-llama/Llama-3.1-8B-Instruct");
    }

    private AppConfig() {}
}
