// Point this to your hosted Java backend IP or Domain
const BACKEND_URL = "http://localhost:8080/api/adapt-ui";

let currentPupil = 4.0;
let currentGazeX = 500;
let currentGazeY = 300;

// Track cursor as retina gaze proxy
document.addEventListener("mousemove", (event) => {
    currentGazeX = event.clientX;
    currentGazeY = event.clientY;

    // Simulate pupil adaptation based on distance to center
    const centerDist = Math.hypot(window.innerWidth / 2 - currentGazeX, window.innerHeight / 2 - currentGazeY);
    currentPupil = Number((2.5 + (centerDist / 300)).toFixed(2));
});

async function triggerScan() {
    document.getElementById("val-pupil").innerText = `${currentPupil}mm`;
    document.getElementById("val-gaze").innerText = `${currentGazeX}, ${currentGazeY}`;

    const btn = document.getElementById("scan-btn");
    btn.disabled = true;
    btn.innerText = "Analyzing with Ollama...";

    try {
        const response = await fetch(BACKEND_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                gazeX: currentGazeX,
                gazeY: currentGazeY,
                pupilDiameter: currentPupil
            })
        });

        const theme = await response.json();
        applyTheme(theme);
    } catch (err) {
        console.error("Failed to fetch dynamic UI config:", err);
    } finally {
        btn.disabled = false;
        btn.innerText = "Scan Retina & Adapt Theme";
    }
}

function applyTheme(theme) {
    const root = document.documentElement;
    if (theme.bgColor) root.style.setProperty("--bg-color", theme.bgColor);
    if (theme.textColor) root.style.setProperty("--text-color", theme.textColor);
    if (theme.accentColor) root.style.setProperty("--accent-color", theme.accentColor);
    if (theme.cardBg) root.style.setProperty("--card-bg", theme.cardBg);
    if (theme.fontFamily) root.style.setProperty("--ui-font", theme.fontFamily);
}