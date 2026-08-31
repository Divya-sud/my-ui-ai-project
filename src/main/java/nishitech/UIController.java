package nishitech;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static spark.Spark.*;

public class UIController {
    // Primary Neuromorphic Multi-Vector Engine
    private static final AutonomousUIEngine autonomousEngine = new AutonomousUIEngine();
    // Fallback/Legacy Ocular Service
    private static final OllamaService legacyService = new OllamaService();
    private static final Gson gson = new Gson();

    public static void registerRoutes() {
        path("/api", () -> {
            post("/adapt-ui", (req, res) -> {
                res.type("application/json");

                String body = req.body();
                if (body == null || body.isBlank()) {
                    UserTelemetry defaultTelemetry = new UserTelemetry();
                    defaultTelemetry.ocularProfile = "balanced";
                    return autonomousEngine.generateAutonomousTheme(defaultTelemetry);
                }

                try {
                    // 1. Try parsing full telemetry payload (Ocular + Cookies + Search History)
                    UserTelemetry telemetry = gson.fromJson(body, UserTelemetry.class);

                    // Check if modern multi-vector payload was sent
                    if (telemetry != null && (telemetry.ocularProfile != null || telemetry.searchTerms != null)) {
                        System.out.println("Processing Multi-Vector Telemetry: " + telemetry.ocularProfile);
                        return autonomousEngine.generateAutonomousTheme(telemetry);
                    }

                    // 2. Compatibility Layer: Handle legacy simple payloads {"profile": "..."}
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("profile")) {
                        String profile = json.get("profile").getAsString();
                        System.out.println("Processing Single-Vector Profile: " + profile);

                        // Map into UserTelemetry for the Autonomous Engine
                        UserTelemetry fallbackTelemetry = new UserTelemetry();
                        fallbackTelemetry.ocularProfile = profile;
                        return autonomousEngine.generateAutonomousTheme(fallbackTelemetry);
                    }

                } catch (Exception e) {
                    System.err.println("Telemetry parsing notice: " + e.getMessage());
                }

                // Default safety return if payload format is unrecognized
                UserTelemetry safeFallback = new UserTelemetry();
                safeFallback.ocularProfile = "balanced";
                return autonomousEngine.generateAutonomousTheme(safeFallback);
            });
        });
    }
}