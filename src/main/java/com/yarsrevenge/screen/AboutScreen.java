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

public class AboutScreen implements com.yarsrevenge.screen.Screen {

    private final StackPane root;
    private final Canvas    canvas;
    private AnimationTimer  animTimer;
    private long            startNanos = -1;

    public AboutScreen() {
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

        double cx = GameConstants.LOGICAL_W / 2.0;
        double y  = 110;
        double dy = 62;

        // Title
        double pulse = 0.75 + 0.25 * Math.sin(t * 2.2);
        gc.setFont(GameFont.of(54));
        gc.setFill(Color.rgb((int)(255 * pulse), (int)(110 * pulse), 30));
        centred(gc, "YAR'S REVENGE REMAKE", cx, y); y += 90;

        // Credits block
        gc.setFont(GameFont.of(24));
        gc.setFill(Color.rgb(200, 200, 200));
        centred(gc, "Original Atari 2600 game, 1982", cx, y); y += dy;
        centred(gc, "Remake by Einat,  2026",          cx, y); y += dy + 30;

        // Tech block
        gc.setFont(GameFont.of(20));
        gc.setFill(Color.rgb(140, 220, 255));
        centred(gc, "Programmed with Claude  (Anthropic)", cx, y); y += dy;
        gc.setFill(Color.rgb(180, 180, 180));
        centred(gc, "Graphics: procedural JavaFX vector art", cx, y); y += dy;
        centred(gc, "Audio: PCM synthesized at runtime",      cx, y); y += dy;
        centred(gc, "Engine: JavaFX AnimationTimer, 60 Hz fixed-tick", cx, y); y += dy + 30;

        // Version
        gc.setFont(GameFont.of(22));
        gc.setFill(Color.rgb(255, 220, 50));
        centred(gc, "Version 1.0", cx, y);

        // Footer
        gc.setFont(GameFont.of(16));
        gc.setFill(Color.rgb(120, 120, 120));
        gc.fillText("ENTER / SPACE / ESC  TO RETURN", 570, GameConstants.LOGICAL_H - 40);

        gc.restore();
    }

    private void centred(GraphicsContext gc, String text, double cx, double y) {
        // Approximate centering — Press Start 2P is monospaced, ~13px per char at size 1
        double fontSize = gc.getFont().getSize();
        double approxW  = text.length() * fontSize * 0.62;
        gc.fillText(text, cx - approxW / 2.0, y);
    }
}
