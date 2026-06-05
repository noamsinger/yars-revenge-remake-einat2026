package com.yarsrevenge.screen;

import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.audio.AudioManager;
import com.yarsrevenge.audio.SoundEffect;
import com.yarsrevenge.model.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

public class MainMenuScreen implements com.yarsrevenge.screen.Screen {

    private final StackPane root;
    private final Canvas canvas;
    private AnimationTimer animTimer;
    private long startNanos = -1;

    // When non-null, we are showing the game-over variant of this screen
    private final Integer gameOverScore;
    private final Integer gameOverWave;

    private int selectedIndex = 0;
    private static final String[] ITEMS = {
        "START GAME", "CONFIGURATION", "HIGH SCORES", "INSTRUCTIONS", "ABOUT", "QUIT"
    };

    private static final Image BG_IMAGE = new Image(
        MainMenuScreen.class.getResourceAsStream("/background.png"));

    public MainMenuScreen() {
        this(null, null);
    }

    public MainMenuScreen(Integer score, Integer wave) {
        this.gameOverScore = score;
        this.gameOverWave  = wave;
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    @Override
    public void show() {
        if (gameOverScore != null) {
            HighScoreScreen.maybeRecord(gameOverScore);
        }
        selectedIndex = 0;
        setupInput();
        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startNanos < 0) startNanos = now;
                double t = (now - startNanos) / 1_000_000_000.0;
                drawMenu(t);
            }
        };
        startNanos = -1;
        animTimer.start();
        AudioManager.getInstance().play(SoundEffect.WAVE_START);
    }

    @Override
    public void hide() {
        if (animTimer != null) animTimer.stop();
        SceneManager.getInstance().getScene().setOnKeyPressed(null);
    }

    @Override
    public Node getRoot() { return root; }

    private void setupInput() {
        SceneManager.getInstance().getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP    || e.getCode() == KeyCode.W) {
                selectedIndex = (selectedIndex - 1 + ITEMS.length) % ITEMS.length;
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.S) {
                selectedIndex = (selectedIndex + 1) % ITEMS.length;
            } else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                activate();
            }
        });
    }

    private void activate() {
        switch (selectedIndex) {
            case 0 -> SceneManager.getInstance().showGame();
            case 1 -> SceneManager.getInstance().showConfig();
            case 2 -> SceneManager.getInstance().showHighScores();
            case 3 -> SceneManager.getInstance().showInstructions();
            case 4 -> SceneManager.getInstance().showAbout();
            case 5 -> { javafx.application.Platform.exit(); System.exit(0); }
        }
    }

    private void drawMenu(double t) {
        double physW = canvas.getWidth();
        double physH = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, physW, physH);
        gc.save();
        gc.scale(physW / GameConstants.LOGICAL_W, physH / GameConstants.LOGICAL_H);

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

        if (BG_IMAGE != null && !BG_IMAGE.isError()) {
            gc.drawImage(BG_IMAGE, 0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);
        }

        if (gameOverScore != null) {
            drawGameOverVariant(gc, t);
        } else {
            drawNormalVariant(gc, t);
        }

        gc.restore();
    }

    private void drawGlassPanel(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRoundRect(x, y, w, h, 18, 18);
        gc.setStroke(Color.rgb(255, 255, 255, 0.08));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 18, 18);
    }

    private void drawNormalVariant(GraphicsContext gc, double t) {
        // Glass behind title block
        drawGlassPanel(gc, 80, 80, 1100, 240);

        // Title
        gc.setFont(com.yarsrevenge.renderer.GameFont.of(72));
        double pulse = 0.7 + 0.3 * Math.sin(t * 2.5);
        gc.setFill(Color.rgb((int)(255 * pulse), (int)(100 * pulse), 30));
        gc.fillText("YAR'S REVENGE", 120, 190);

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(24));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("ATARI 2600 REMAKE", 460, 260);

        drawMenuItems(gc, t, 400);

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(14));
        gc.setFill(Color.rgb(120, 120, 120));
        gc.fillText("ARROWS/WASD: NAV   ENTER/SPACE: SELECT",
                    350, GameConstants.LOGICAL_H - 40);
    }

    private void drawGameOverVariant(GraphicsContext gc, double t) {
        // Background dim: 0–10s fully dimmed to 10% brightness, 10–15s fade back to normal
        double dimAlpha;
        if (t < 10.0) {
            dimAlpha = 0.90; // 90% black overlay = 10% brightness
        } else if (t < 15.0) {
            dimAlpha = 0.90 * (1.0 - (t - 10.0) / 5.0);
        } else {
            dimAlpha = 0.0;
        }
        if (dimAlpha > 0) {
            gc.setFill(Color.rgb(0, 0, 0, dimAlpha));
            gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);
        }

        // GAME OVER banner on a vertical figure-8 (lemniscate) path, always on screen
        // Parametric lemniscate rotated 90°: x = A*cos(θ)/(1+sin²(θ)), y = A*sin(θ)cos(θ)/(1+sin²(θ))
        // θ advances with time; A scaled to keep within screen
        double theta = t * 0.7; // period ~9s
        double sinT = Math.sin(theta), cosT = Math.cos(theta);
        double denom = 1.0 + sinT * sinT;
        double lemnA = 340.0; // horizontal half-amplitude
        double lemnB = 260.0; // vertical half-amplitude
        // Vertical figure-8: swap x/y roles
        double bx = GameConstants.LOGICAL_W / 2.0 + lemnA * sinT * cosT / denom;
        double by = GameConstants.LOGICAL_H / 2.0 + lemnB * cosT / denom;
        double swingAngle = 12.0 * Math.sin(t * 1.3); // gentle tilt while moving
        double cycle = 0.5 + 0.5 * Math.sin(t * 2.2);
        int r = (int)(160 + 95 * cycle);
        int g2 = (int)(10 + 20 * cycle);
        gc.save();
        gc.translate(bx, by);
        gc.rotate(swingAngle);
        gc.setFont(com.yarsrevenge.renderer.GameFont.of(130));
        gc.setGlobalAlpha(0.40);
        gc.setFill(Color.rgb(r, g2, 10));
        gc.fillText("GAME OVER", -650, 55);
        gc.setGlobalAlpha(1.0);
        gc.restore();

        // Title smaller
        gc.setFont(com.yarsrevenge.renderer.GameFont.of(40));
        double pulse = 0.7 + 0.3 * Math.sin(t * 2.5);
        gc.setFill(Color.rgb((int)(255 * pulse), (int)(100 * pulse), 30));
        gc.fillText("YAR'S REVENGE", 380, 250);

        // Stats
        drawGlassPanel(gc, 620, 310, 720, 160);
        gc.setFont(com.yarsrevenge.renderer.GameFont.of(30));
        gc.setFill(Color.WHITE);
        gc.fillText("SCORE: " + gameOverScore,              680, 370);
        gc.fillText("WAVES: " + (gameOverWave - 1), 680, 430);

        drawMenuItems(gc, t, 490);

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(14));
        gc.setFill(Color.rgb(120, 120, 120));
        gc.fillText("ARROWS/WASD: NAV   ENTER/SPACE: SELECT",
                    350, GameConstants.LOGICAL_H - 40);
    }

    private void drawMenuItems(GraphicsContext gc, double t, double startY) {
        // Glass panel behind the menu items
        drawGlassPanel(gc, 580, startY - 60, 760, ITEMS.length * 80 + 40);

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(30));
        for (int i = 0; i < ITEMS.length; i++) {
            double itemY = startY + i * 80;
            boolean selected = (i == selectedIndex);
            double pulse2 = selected ? (0.7 + 0.3 * Math.sin(t * 6)) : 1.0;
            gc.setFill(selected
                ? Color.rgb((int)(255 * pulse2), (int)(255 * pulse2), 50)
                : Color.rgb(160, 160, 160));
            gc.fillText((selected ? "> " : "  ") + ITEMS[i], 640, itemY);
        }
    }

    private void drawRainbowRibbon(GraphicsContext gc) {
        double W = GameConstants.LOGICAL_W;
        double H = GameConstants.LOGICAL_H;

        // Real rainbow: concentric arcs centered at bottom-right corner (W, H).
        // The arc sweeps 90° — from 180° (pointing left, reaching right-midpoint)
        // to 270° (pointing up, reaching bottom-midpoint).
        // Red is outermost (largest radius ~H/2), violet innermost.
        Color[] rainbow = {
            Color.rgb(255,30,30),   // red      — outermost
            Color.rgb(255,130,20),  // orange
            Color.rgb(255,240,30),  // yellow
            Color.rgb(40,210,40),   // green
            Color.rgb(30,180,255),  // blue
            Color.rgb(60,60,255),   // indigo
            Color.rgb(180,30,255)   // violet   — innermost
        };

        double bandW  = 55.0;
        double outerR = H / 2.0 + bandW * (rainbow.length / 2.0);

        gc.save();
        // Clip to the lower-right triangle so arcs only show in that corner region
        gc.beginPath();
        gc.moveTo(W / 2.0, H);
        gc.lineTo(W,       H);
        gc.lineTo(W,       H / 2.0);
        gc.closePath();
        gc.clip();

        for (int i = 0; i < rainbow.length; i++) {
            double r = outerR - i * bandW;
            gc.setStroke(rainbow[i].deriveColor(0, 1, 1, 0.85));
            gc.setLineWidth(bandW - 1);
            // strokeArc: bounding box top-left relative to center (W, H)
            gc.strokeArc(W - r, H - r, r * 2, r * 2,
                         180.0, 90.0,
                         javafx.scene.shape.ArcType.OPEN);
        }
        gc.restore();
    }

    private void drawStars(GraphicsContext gc, double t) {
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 200; i++) {
            double sx = rng.nextDouble() * GameConstants.LOGICAL_W;
            double sy = rng.nextDouble() * GameConstants.LOGICAL_H;
            double twinkle = 0.3 + 0.7 * Math.abs(Math.sin(t * (1.0 + rng.nextDouble() * 3) + i));
            gc.setGlobalAlpha(twinkle * 0.8);
            gc.setFill(Color.WHITE);
            gc.fillOval(sx, sy, 2, 2);
        }
        gc.setGlobalAlpha(1.0);
    }
}
