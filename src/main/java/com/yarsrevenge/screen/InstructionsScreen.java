package com.yarsrevenge.screen;

import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.renderer.GameFont;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

public class InstructionsScreen implements com.yarsrevenge.screen.Screen {

    private final StackPane root;
    private final Canvas    canvas;
    private AnimationTimer  animTimer;
    private long            startNanos = -1;

    public InstructionsScreen() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        root   = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    @Override
    public void show() {
        SceneManager.getInstance().getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER
                    || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.BACK_SPACE) {
                SceneManager.getInstance().showMainMenu();
            }
        });
        animTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (startNanos < 0) startNanos = now;
                draw((now - startNanos) / 1_000_000_000.0);
            }
        };
        startNanos = -1;
        animTimer.start();
    }

    @Override
    public void hide() {
        if (animTimer != null) animTimer.stop();
        SceneManager.getInstance().getScene().setOnKeyPressed(null);
    }

    @Override
    public Node getRoot() { return root; }

    private void draw(double t) {
        double physW = canvas.getWidth();
        double physH = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, physW, physH);
        gc.save();
        gc.scale(physW / GameConstants.LOGICAL_W, physH / GameConstants.LOGICAL_H);

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

        // Title
        gc.setFont(GameFont.of(48));
        gc.setFill(Color.rgb(255, 220, 50));
        gc.fillText("INSTRUCTIONS", 380, 100);

        double lx = 120, rx = 560;
        double y  = 195;
        double dy = 52;

        // Section: CONTROLS
        section(gc, "CONTROLS", lx, y); y += 58;
        row(gc, "ARROW KEYS / WASD", "MOVE", lx, rx, y); y += dy;
        row(gc, "ENTER",             "FIRE BULLET", lx, rx, y); y += dy;
        row(gc, "SPACE",             "ZORLON CANNON  (when charged)", lx, rx, y); y += dy;
        row(gc, "ESC",               "PAUSE", lx, rx, y); y += dy;
        row(gc, "M",                 "MINIMIZE  (minimize window)", lx, rx, y); y += dy + 24;

        // Section: OBJECTIVE
        section(gc, "OBJECTIVE", lx, y); y += 58;
        bullet(gc, "Eat through the shield using the Swirl or your bullets.", lx, y); y += dy;
        bullet(gc, "Expose the Quotile, then fire the Zorlon Cannon to destroy it.", lx, y); y += dy;
        bullet(gc, "Avoid enemy bullets, the Swirl, and the Quotile missile.", lx, y); y += dy + 24;

        // Section: SCORING
        section(gc, "SCORING", lx, y); y += 58;
        row(gc, "Shield cell eaten",  "10 pts",     lx, rx, y); y += dy;
        row(gc, "Swirl destroyed",    "500 pts",    lx, rx, y); y += dy;
        row(gc, "Quotile destroyed",  "2000 pts + wave bonus", lx, rx, y);

        // Footer
        gc.setFont(GameFont.of(16));
        gc.setFill(Color.rgb(120, 120, 120));
        gc.fillText("ENTER / SPACE / ESC  TO RETURN", 570, GameConstants.LOGICAL_H - 40);

        gc.restore();
    }

    private void section(GraphicsContext gc, String label, double x, double y) {
        gc.setFont(GameFont.of(26));
        gc.setFill(Color.rgb(255, 160, 40));
        gc.fillText(label, x, y);
    }

    private void row(GraphicsContext gc, String key, String desc, double lx, double rx, double y) {
        gc.setFont(GameFont.of(20));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText(key, lx + 30, y);
        gc.setFill(Color.rgb(140, 220, 255));
        gc.fillText(desc, rx, y);
    }

    private void bullet(GraphicsContext gc, String text, double lx, double y) {
        gc.setFont(GameFont.of(18));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("*  " + text, lx + 30, y);
    }
}
