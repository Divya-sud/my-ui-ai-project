package nishitech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static spark.Spark.post;

public class UIController {
    private static final OllamaService ollamaService = new OllamaService();

    public static void registerRoutes() {
        post("/api/adapt-ui", (req, res) -> {
            JsonObject body = JsonParser.parseString(req.body()).getAsJsonObject();

            double gazeX = body.has("gazeX") ? body.get("gazeX").getAsDouble() : 500.0;
            double gazeY = body.has("gazeY") ? body.get("gazeY").getAsDouble() : 300.0;
            double pupilDiameter = body.has("pupilDiameter") ? body.get("pupilDiameter").getAsDouble() : 4.0;

            String visionState = RetinaProcessor.analyzeGazeData(gazeX, gazeY, pupilDiameter);
            return ollamaService.generateThemeFromRetina(visionState);
        });
    }
}