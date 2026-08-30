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
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        Properties prop = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) prop.load(in);
        } catch (Exception ignored) {}

        this.apiUrl = prop.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        this.model = prop.getProperty("groq.model", "llama-3.3-70b-versatile");

        String envKey = System.getenv("GROQ_API_KEY");
        this.apiKey = (envKey != null && !envKey.isBlank()) ? envKey : prop.getProperty("groq.api.key", "");
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            return getFallbackTheme("No Groq Key Configured");
        }

        String prompt = "You are a world-class biometric vision UI ergonomics engine for BharatAcre.com. "
                + "The user's eye and gaze sensor profile is: '" + visionProfile + "'. "
                + "Rules: "
                + "1. If profile indicates 'squinting' or 'strain', increase fontSize to '18px' or '19px', use higher contrast foreground text (#FFFFFF). "
                + "2. If profile indicates 'far-distance', boost font weight and scale typography to '19px'. "
                + "3. If 'night-strain-relief', use deep slate OLED black (#080c14), warm amber accent (#f59e0b), and soft off-white text (#f1f5f9). "
                + "Output ONLY raw JSON matching this schema: "
                + "{"
                + "\"bgMain\":\"#hex\","
                + "\"surface\":\"#hex\","
                + "\"surfaceBorder\":\"rgba(..)\","
                + "\"textPrimary\":\"#hex\","
                + "\"textSecondary\":\"#hex\","
                + "\"accent\":\"#hex\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"Brief explanation of optical adaptation\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You are an automated API endpoint. Return raw JSON matching the schema only. No markdown formatting, no explanations.");

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
                    .header("Authorization", "Bearer " + this.apiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq HTTP " + response.statusCode() + ": " + response.body());
                return getFallbackTheme("HTTP " + response.statusCode());
            }

            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackTheme("Engine exception");
        }
    }

    private String getFallbackTheme(String status) {
        return "{"
                + "\"bgMain\":\"#0b0f19\","
                + "\"surface\":\"#131b2e\","
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