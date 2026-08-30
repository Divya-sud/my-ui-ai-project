package nishitech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static spark.Spark.*;

public class UIController {
    private static final OllamaService aiService = new OllamaService();

    public static void registerRoutes() {
        path("/api", () -> {
            post("/adapt-ui", (req, res) -> {
                res.type("application/json");

                String requestBody = req.body();
                String profile = "standard-comfortable";

                try {
                    if (requestBody != null && !requestBody.isBlank()) {
                        JsonObject obj = JsonParser.parseString(requestBody).getAsJsonObject();
                        if (obj.has("profile")) {
                            profile = obj.get("profile").getAsString();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse request JSON: " + e.getMessage());
                }

                System.out.println("Processing Biometric Vision Profile: " + profile);
                String generatedTokens = aiService.generateThemeFromRetina(profile);
                return generatedTokens;
            });
        });
    }
}