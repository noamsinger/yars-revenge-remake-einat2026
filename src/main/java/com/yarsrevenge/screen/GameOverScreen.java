package com.yarsrevenge.screen;

import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.model.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

public class GameOverScreen implements com.yarsrevenge.screen.Screen {

    private final StackPane root;
    private final Canvas canvas;
    private final int score;
    private final int wave;
    private AnimationTimer animTimer;
    private long startNanos = -1;

    private int selectedIndex = 0;
    private static final String[] ITEMS = {"PLAY AGAIN", "MAIN MENU"};

    public GameOverScreen(int score, int wave) {
        this.score = score;
        this.wave  = wave;

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    @Override
    public void show() {
        HighScoreScreen.maybeRecord(score);
        setupInput();
        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startNanos < 0) startNanos = now;
                double t = (now - startNanos) / 1_000_000_000.0;
                draw(t);
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

    private void setupInput() {
        SceneManager.getInstance().getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP   || e.getCode() == KeyCode.W) {
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
            case 1 -> SceneManager.getInstance().showMainMenu();
        }
    }

    private void draw(double t) {
        double physW = canvas.getWidth();
        double physH = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, physW, physH);
        gc.save();
        gc.scale(physW / GameConstants.LOGICAL_W, physH / GameConstants.LOGICAL_H);

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

        // "GAME OVER" title flashing
        double flash = 0.5 + 0.5 * Math.sin(t * 4);
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 110));
        gc.setFill(Color.rgb(255, (int)(50 * flash), 30));
        gc.fillText("GAME OVER", 340, 220);

        // Stats
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 48));
        gc.setFill(Color.WHITE);
        gc.fillText("SCORE: " + score,          680, 360);
        gc.fillText("WAVES CLEARED: " + (wave - 1), 680, 430);

        // Menu items
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 50));
        for (int i = 0; i < ITEMS.length; i++) {
            double itemY = 620 + i * 90;
            boolean sel = (i == selectedIndex);
            double p = sel ? (0.7 + 0.3 * Math.sin(t * 6)) : 1.0;
            gc.setFill(sel ? Color.rgb((int)(255*p),(int)(255*p),50) : Color.rgb(160,160,160));
            gc.fillText((sel ? "> " : "  ") + ITEMS[i], 760, itemY);
        }

        gc.restore();
    }
}
