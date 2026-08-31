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
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String ACTIVE_MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client;
    private final String apiKey;

    public OllamaService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();

        String key = System.getenv("GROQ_API_KEY");
        this.apiKey = (key != null) ? key.trim() : "";
    }

    public String generateThemeFromRetina(String visionProfile) {
        if (this.apiKey.isEmpty()) {
            return generateDeterministicTokens(visionProfile, "Local Fallback (Missing Key)");
        }

        String prompt = "You are an AI ergonomics engine for BharatAcre.com. Biometric profile: '"
                + visionProfile + "'. Return ONLY a JSON object matching this schema: "
                + "{"
                + "\"bgMain\":\"#hex\","
                + "\"surface\":\"#hex\","
                + "\"surfaceBorder\":\"rgba(..)\","
                + "\"textPrimary\":\"#hex\","
                + "\"textSecondary\":\"#hex\","
                + "\"accent\":\"#hex\","
                + "\"fontSize\":\"16px\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"Ocular Sync Active\""
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "Output strictly valid JSON matching the schema. Do not include markdown code blocks or prose.");

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
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                System.err.println("Groq HTTP " + response.statusCode() + ": " + response.body());
                return generateDeterministicTokens(visionProfile, "Groq HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Groq Network Error: " + e.getMessage());
            return generateDeterministicTokens(visionProfile, "Network Timeout");
        }
    }

    private String generateDeterministicTokens(String profile, String origin) {
        String bg, surface, textPri, textSec, accent, size, font;

        switch (profile) {
            case "squinting-high-strain":
                bg = "#000000";
                surface = "#111827";
                textPri = "#ffffff";
                textSec = "#cbd5e1";
                accent = "#ef4444";
                size = "19px";
                font = "'Plus Jakarta Sans', sans-serif";
                break;
            case "low-light-retina-strain":
                bg = "#030712";
                surface = "#0f172a";
                textPri = "#f8fafc";
                textSec = "#94a3b8";
                accent = "#f59e0b";
                size = "16px";
                font = "'Plus Jakarta Sans', sans-serif";
                break;
            case "high-glare-ambient":
                bg = "#ffffff";
                surface = "#f1f5f9";
                textPri = "#020617";
                textSec = "#334155";
                accent = "#2563eb";
                size = "17px";
                font = "'Plus Jakarta Sans', sans-serif";
                break;
            default:
                bg = "#080c14";
                surface = "#111827";
                textPri = "#ffffff";
                textSec = "#94a3b8";
                accent = "#dc2626";
                size = "16px";
                font = "'Plus Jakarta Sans', sans-serif";
                break;
        }

        return "{"
                + "\"bgMain\":\"" + bg + "\","
                + "\"surface\":\"" + surface + "\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.12)\","
                + "\"textPrimary\":\"" + textPri + "\","
                + "\"textSecondary\":\"" + textSec + "\","
                + "\"accent\":\"" + accent + "\","
                + "\"fontSize\":\"" + size + "\","
                + "\"fontFamily\":\"" + font + "\","
                + "\"status\":\"Ocular Adaptation (" + origin + ")\""
                + "}";
    }
}