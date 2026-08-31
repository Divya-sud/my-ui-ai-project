package nishitech;

import java.util.List;

public class UserTelemetry {
    public String ocularProfile;      // "squinting-fatigue", "high-glare", "balanced"
    public double pupilShadowRatio;   // Iris aperture approximation
    public double ambientLuma;        // Light intensity reaching eyes
    public List<String> searchTerms;  // "luxury villa", "cheap plot", "commercial"
    public String currentCategory;    // "Land", "Apartment", "Builder Floor"
    public int sessionDwellSeconds;   // Fatigue accumulation metric
    public String preferredCity;      // Extracted from user cookie
}