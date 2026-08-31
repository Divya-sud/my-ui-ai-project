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
    // Hardcode the exact verified URL and model to prevent 404s
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";

    private final String apiUrl;
    private final String model;
    private final String apiKey;
    private final HttpClient client;

    public OllamaService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Direct assignment guarantees no trailing slashes or null paths
        this.apiUrl = GROQ_ENDPOINT;
        this.model = DEFAULT_MODEL;

        // Resolve API key safely
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey == null || envKey.isBlank()) {
            Properties prop = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) prop.load(in);
            } catch (Exception ignored) {}
            envKey = prop.getProperty("groq.api.key", "");
        }

        this.apiKey = (envKey != null) ? envKey.trim() : "";
        System.out.println("OllamaService Active -> Endpoint: " + this.apiUrl + " | Model: " + this.model);
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey.isBlank()) {
            System.err.println("CRITICAL: GROQ_API_KEY is not set in Render!");
            return getFallbackTheme("Missing GROQ_API_KEY");
        }

        String prompt = "You are an adaptive visual ergonomics engine for BharatAcre.com. "
                + "User biometric vision profile: '" + visionProfile + "'. "
                + "Generate an optimal UI theme. "
                + "Output ONLY a valid JSON object matching this schema: "
                + "{"
                + "\"bgMain\":\"#hex\","
                + "\"surface\":\"#hex\","
                + "\"surfaceBorder\":\"rgba(..)\","
                + "\"textPrimary\":\"#hex\","
                + "\"textSecondary\":\"#hex\","
                + "\"accent\":\"#hex\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"Retina adaptive profile active\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You are an automated API. Output strictly raw JSON matching the requested schema. No markdown formatting, no commentary.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", this.model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.1);

        JsonObject jsonFormat = new JsonObject();
        jsonFormat.addProperty("type", "json_object");
        body.add("response_format", jsonFormat);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.apiUrl))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq API Rejection [" + response.statusCode() + "]: " + response.body());
                return getFallbackTheme("Groq HTTP " + response.statusCode());
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            System.err.println("Error connecting to Groq: " + e.getMessage());
            return getFallbackTheme("Connection Error");
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