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
    private final String apiUrl;
    private final String model;
    private final String apiKey;
    private final HttpClient client;

    public OllamaService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        Properties prop = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) prop.load(in);
        } catch (Exception ignored) {}

        this.apiUrl = prop.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        this.model = prop.getProperty("groq.model", "llama-3.3-70b-versatile");

        // Reads key from environment variable (works in Render and locally)
        String envKey = System.getenv("GROQ_API_KEY");
        this.apiKey = (envKey != null && !envKey.isBlank()) ? envKey : prop.getProperty("groq.api.key", "");
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            return getFallbackTheme("No Groq API key set. Set GROQ_API_KEY env var.");
        }

        String prompt = "You are an adaptive visual ergonomics UX engine for real estate portal BharatAcre.com. "
                + "The user's eye and gaze profile indicates: '" + visionProfile + "'. "
                + "Generate an optimal color palette and typography configuration to reduce eye strain. "
                + "Return ONLY a valid JSON object matching this exact schema: "
                + "{"
                + "\"bgMain\":\"#hex\","
                + "\"surface\":\"#hex\","
                + "\"surfaceBorder\":\"#hex\","
                + "\"textPrimary\":\"#hex\","
                + "\"textSecondary\":\"#hex\","
                + "\"accent\":\"#hex\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"Inter, sans-serif\","
                + "\"status\":\"Vision profile active\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You output strictly raw JSON matching the requested schema. Do not write markdown blocks or commentary.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", this.model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.2);

        JsonObject jsonFormat = new JsonObject();
        jsonFormat.addProperty("type", "json_object");
        body.add("response_format", jsonFormat);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.apiUrl))
                    .header("Authorization", "Bearer " + this.apiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq API Error [" + response.statusCode() + "]: " + response.body());
                return getFallbackTheme("Groq HTTP " + response.statusCode());
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackTheme("Connection exception");
        }
    }

    private String getFallbackTheme(String reason) {
        return "{"
                + "\"bgMain\":\"#0b0f19\","
                + "\"surface\":\"#131b2e\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.08)\","
                + "\"textPrimary\":\"#f8fafc\","
                + "\"textSecondary\":\"#94a3b8\","
                + "\"accent\":\"#dc2626\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"Inter, sans-serif\","
                + "\"status\":\"" + reason + "\""
                + "}";
    }
}