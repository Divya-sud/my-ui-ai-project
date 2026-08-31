package nishitech;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AutonomousUIEngine {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client;
    private final String apiKey;

    public AutonomousUIEngine() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        String key = System.getenv("GROQ_API_KEY");
        this.apiKey = (key != null) ? key.trim() : "";
    }

    public String generateAutonomousTheme(UserTelemetry data) {
        if (this.apiKey.isEmpty()) {
            return generateDeterministicSafetyTokens(data);
        }

        String searchContext = (data.searchTerms != null && !data.searchTerms.isEmpty())
                ? String.join(", ", data.searchTerms)
                : "general real estate browsing";

        String prompt = "You are the Autonomous Neuromorphic UI Engine for BharatAcre.com.\n"
                + "User Biometric & Intent Vectors:\n"
                + "- Ocular Biometric Status: " + data.ocularProfile + "\n"
                + "- Pupil Shadow Ratio: " + data.pupilShadowRatio + "\n"
                + "- Ambient Luminescence: " + data.ambientLuma + "\n"
                + "- Recent User Intent / Searches: " + searchContext + "\n"
                + "- Current Browsing Category: " + data.currentCategory + "\n"
                + "- Screen Dwell Fatigue: " + data.sessionDwellSeconds + "s\n\n"
                + "Optimization Requirements:\n"
                + "1. Ocular Relief: If ocularProfile is 'squinting-fatigue' or dwell > 600s, set fontSize to 18px or 19px and use maximum contrast text on black (#000000).\n"
                + "2. Intent Styling: If searching 'luxury' or 'villa', generate gold/champagne accents (#d97706, #f59e0b) and elegant dark slate surfaces (#0b0f19). If 'land' or 'plots', use emerald accents (#059669, #10b981).\n"
                + "3. Glare Control: If ambientLuma > 180, use pure white background (#ffffff) and dark ink text (#020617).\n\n"
                + "Respond ONLY with a valid JSON matching this schema:\n"
                + "{\n"
                + "\"bgMain\":\"#hex\",\n"
                + "\"surface\":\"#hex\",\n"
                + "\"surfaceBorder\":\"rgba(..)\",\n"
                + "\"textPrimary\":\"#hex\",\n"
                + "\"textSecondary\":\"#hex\",\n"
                + "\"accent\":\"#hex\",\n"
                + "\"fontSize\":\"16px\",\n"
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\",\n"
                + "\"status\":\"Live explanation of the adaptation\"\n"
                + "}";

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "You output raw JSON matching the requested schema strictly. Do not include markdown or conversational formatting.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.15);

        JsonObject jsonFormat = new JsonObject();
        jsonFormat.addProperty("type", "json_object");
        body.add("response_format", jsonFormat);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + this.apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            }
            return generateDeterministicSafetyTokens(data);
        } catch (Exception e) {
            return generateDeterministicSafetyTokens(data);
        }
    }

    private String generateDeterministicSafetyTokens(UserTelemetry data) {
        boolean isSquinting = "squinting-fatigue".equals(data.ocularProfile);
        String bg = isSquinting ? "#000000" : "#080c14";
        String surface = isSquinting ? "#0f172a" : "#111827";
        String accent = (data.currentCategory != null && data.currentCategory.equalsIgnoreCase("Land")) ? "#10b981" : "#dc2626";
        String size = isSquinting ? "19px" : "16px";

        return "{"
                + "\"bgMain\":\"" + bg + "\","
                + "\"surface\":\"" + surface + "\","
                + "\"surfaceBorder\":\"rgba(255,255,255,0.12)\","
                + "\"textPrimary\":\"#ffffff\","
                + "\"textSecondary\":\"#94a3b8\","
                + "\"accent\":\"" + accent + "\","
                + "\"fontSize\":\"" + size + "\","
                + "\"fontFamily\":\"'Plus Jakarta Sans', sans-serif\","
                + "\"status\":\"Autonomous Neuromorphic Sync (Edge Local)\""
                + "}";
    }
}