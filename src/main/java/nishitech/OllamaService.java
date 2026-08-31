package nishitech;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaService {
    // Official, permanent Groq production endpoint
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    // Verified active Groq model
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client;
    private final String apiKey;

    public OllamaService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String envKey = System.getenv("GROQ_API_KEY");
        this.apiKey = (envKey != null) ? envKey.trim() : "";
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey.isBlank()) {
            return getFallbackTheme("Missing API Key");
        }

        String prompt = "Generate an adaptive visual theme for website BharatAcre.com based on biometric status: '"
                + visionProfile + "'. Output strictly valid JSON matching this schema: "
                + "{"
                + "\"bgMain\":\"#080c14\","
                + "\"surface\":\"#111827\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.12)\","
                + "\"textPrimary\":\"#ffffff\","
                + "\"textSecondary\":\"#94a3b8\","
                + "\"accent\":\"#dc2626\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"Vision profile active\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You output raw JSON matching the requested schema only. No markdown formatting.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", GROQ_MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        JsonObject jsonFormat = new JsonObject();
        jsonFormat.addProperty("type", "json_object");
        body.add("response_format", jsonFormat);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_ENDPOINT))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq HTTP " + response.statusCode() + ": " + response.body());
                return getFallbackTheme("Groq HTTP " + response.statusCode());
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            return getFallbackTheme("Service Offline");
        }
    }

    private String getFallbackTheme(String reason) {
        return "{"
                + "\"bgMain\":\"#080c14\","
                + "\"surface\":\"#111827\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.12)\","
                + "\"textPrimary\":\"#ffffff\","
                + "\"textSecondary\":\"#94a3b8\","
                + "\"accent\":\"#dc2626\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"" + reason + "\""
                + "}";
    }
}