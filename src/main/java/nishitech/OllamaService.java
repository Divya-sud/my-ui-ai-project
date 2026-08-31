package nishitech;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class OllamaService {
    // Locked endpoint (strictly no trailing slash)
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Universally supported active model on Groq Cloud
    private static final String ACTIVE_MODEL = "llama3-70b-8192";

    private final HttpClient client;
    private final String apiKey;

    public OllamaService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String key = System.getenv("GROQ_API_KEY");
        if (key == null || key.isBlank()) {
            Properties prop = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) prop.load(in);
            } catch (Exception ignored) {}
            key = prop.getProperty("groq.api.key", "");
        }
        this.apiKey = (key != null) ? key.trim() : "";
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey.isEmpty()) {
            return getFallbackTheme("Missing GROQ_API_KEY");
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
                + "\"status\":\"Retina adaptive profile active\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You output raw JSON matching the schema only. No markdown fences, no conversational prose.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", ACTIVE_MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        JsonObject jsonFormat = new JsonObject();
        jsonFormat.addProperty("type", "json_object");
        body.add("response_format", jsonFormat);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq HTTP " + response.statusCode() + " Details: " + response.body());
                return getFallbackTheme("Groq HTTP " + response.statusCode());
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            return getFallbackTheme("Connection Exception");
        }
    }

    private String getFallbackTheme(String status) {
        return "{"
                + "\"bgMain\":\"#080c14\","
                + "\"surface\":\"#111827\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.12)\","
                + "\"textPrimary\":\"#ffffff\","
                + "\"textSecondary\":\"#94a3b8\","
                + "\"accent\":\"#dc2626\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"" + status + "\""
                + "}";
    }
}