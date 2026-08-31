package nishitech;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int portNumber = (portStr != null) ? Integer.parseInt(portStr) : 8080;
        port(portNumber);

        // Global CORS headers
        options("/*", (req, res) -> {
            String headers = req.headers("Access-Control-Request-Headers");
            if (headers != null) res.header("Access-Control-Allow-Headers", headers);
            String method = req.headers("Access-Control-Request-Method");
            if (method != null) res.header("Access-Control-Allow-Methods", method);
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
            res.type("application/json");
        });

        // Root status endpoint to clear the unmapped route notice
        get("/", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ONLINE\",\"service\":\"BharatAcre AI Vision Engine\"}";
        });

        get("/health", (req, res) -> "{\"status\":\"UP\",\"provider\":\"Groq-Cloud\"}");

        UIController.registerRoutes();

        System.out.println("BharatAcre AI Backend running on port " + portNumber);
    }
}