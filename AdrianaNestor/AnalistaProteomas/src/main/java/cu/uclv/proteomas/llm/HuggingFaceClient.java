package cu.uclv.proteomas.llm;

import com.google.gson.*;
import cu.uclv.proteomas.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente para la API de Hugging Face, usando el endpoint de Chat Completions
 * (compatible con modelos como Llama-3.1-8B-Instruct). Envía un arreglo de
 * mensajes con roles "system" y "user", junto con parámetros de generación.
 */
public class HuggingFaceClient {

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public HuggingFaceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String generarRazonamiento(String promptSistema, String promptUsuario)
            throws IOException, InterruptedException {

        String url = AppConfig.hfApiUrl();

        JsonArray messages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", promptSistema);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", promptUsuario);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", AppConfig.hfModel());
        body.add("messages", messages);
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + AppConfig.hfApiKey())
                .timeout(Duration.ofSeconds(300))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Hugging Face API respondió " + response.statusCode() +
                    ": " + response.body());
        }

        return extraerTexto(response.body());
    }

    private String extraerTexto(String jsonRespuesta) throws IOException {
        JsonObject root = JsonParser.parseString(jsonRespuesta).getAsJsonObject();

        if (root.has("choices")) {
            JsonObject firstChoice = root.getAsJsonArray("choices").get(0).getAsJsonObject();
            return firstChoice.getAsJsonObject("message")
                    .get("content").getAsString().trim();
        }

        if (root.has("error")) {
            throw new IOException("Error de Hugging Face: " + root.get("error"));
        }

        throw new IOException("No se pudo extraer el texto de la respuesta de HF: " + jsonRespuesta);
    }
}