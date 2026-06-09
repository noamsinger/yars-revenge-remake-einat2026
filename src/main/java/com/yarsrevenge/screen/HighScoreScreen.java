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

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HighScoreScreen implements com.yarsrevenge.screen.Screen {

    private static final Path SCORE_FILE = Path.of(System.getProperty("user.home"), ".yars-revenge-scores");
    private static final int MAX_SCORES = 10;

    private final StackPane root;
    private final Canvas canvas;
    private AnimationTimer animTimer;
    private long startNanos = -1;
    private List<Integer> scores;

    public HighScoreScreen() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    public static void maybeRecord(int score) {
        List<Integer> scores = loadScores();
        scores.add(score);
        scores.sort(Comparator.reverseOrder());
        if (scores.size() > MAX_SCORES) scores = scores.subList(0, MAX_SCORES);
        saveScores(scores);
    }

    private static List<Integer> loadScores() {
        List<Integer> list = new ArrayList<>();
        if (Files.exists(SCORE_FILE)) {
            try (BufferedReader r = Files.newBufferedReader(SCORE_FILE)) {
                String line;
                while ((line = r.readLine()) != null) {
                    try { list.add(Integer.parseInt(line.trim())); } catch (NumberFormatException ignored) {}
                }
            } catch (IOException ignored) {}
        }
        return list;
    }

    private static void saveScores(List<Integer> scores) {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(SCORE_FILE))) {
            for (int s : scores) w.println(s);
        } catch (IOException ignored) {}
    }

    @Override
    public void show() {
        scores = loadScores();
        SceneManager.getInstance().getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.BACK_SPACE
                    || e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                SceneManager.getInstance().showMainMenu();
            }
        });
        canvas.setOnMouseClicked(null);

        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
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

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(52));
        gc.setFill(Color.rgb(255, 220, 50));
        gc.fillText("HIGH EXP", 510, 130);

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(32));
        for (int i = 0; i < scores.size(); i++) {
            double pulse = (i == 0) ? (0.8 + 0.2 * Math.sin(t * 3)) : 1.0;
            gc.setFill(i == 0
                ? Color.rgb((int)(255 * pulse), (int)(220 * pulse), 50)
                : Color.rgb(180, 180, 180));
            gc.fillText(String.format("%2d.  %7d", i + 1, scores.get(i)), 680, 230 + i * 70);
        }
        if (scores.isEmpty()) {
            gc.setFill(Color.rgb(120, 120, 120));
            gc.fillText("NO SCORES YET", 560, 400);
        }

        gc.setFont(com.yarsrevenge.renderer.GameFont.of(18));
        gc.setFill(Color.rgb(120, 120, 120));
        gc.fillText("ENTER/SPACE TO RETURN", 640, GameConstants.LOGICAL_H - 40);

        gc.restore();
    }
}
