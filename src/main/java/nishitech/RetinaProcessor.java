package nishitech;

public class RetinaProcessor {

    public static String analyzeGazeData(double gazeX, double gazeY, double pupilDiameter) {
        // High pupil dilation (> 5.5mm) indicates low light adaptation or cognitive strain
        if (pupilDiameter > 5.5) {
            return "strained, prefers high contrast dark theme, low visual clutter";
        }
        // Small pupil diameter (< 3.0mm) indicates bright environment
        else if (pupilDiameter < 3.0) {
            return "well-lit environment, prefers clean minimal light mode, sharp sans-serif font";
        }
        // Gaze concentrated toward top-right/center indicates casual scanning
        else if (gazeY < 300) {
            return "focused on upper screen, prefers warm pastel modern theme, comfortable serif font";
        }
        else {
            return "balanced vision, modern vibrant aesthetics, dynamic geometric font";
        }
    }
}