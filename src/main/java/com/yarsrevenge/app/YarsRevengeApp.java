package com.yarsrevenge.app;

import com.yarsrevenge.audio.AudioManager;
import com.yarsrevenge.config.GameConfig;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class YarsRevengeApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Load config first — determines resolution & audio state
        GameConfig cfg = GameConfig.getInstance();

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        double physW = bounds.getWidth();
        double physH = bounds.getHeight();

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(root, physW, physH, Color.BLACK);

        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        stage.setScene(scene);
        stage.setTitle("Yar's Revenge");

        try (var iconStream = YarsRevengeApp.class.getResourceAsStream("/icon.png")) {
            if (iconStream != null) stage.getIcons().add(new Image(iconStream));
        } catch (Exception ignored) {}

        // Apply resolution from config
        applyResolution(stage, cfg, physW, physH);

        stage.show();

        // Pre-warm audio engine on the FX thread, then apply mute from config
        AudioManager.getInstance().setMuted(!cfg.isAudioEnabled());

        SceneManager.init(stage, scene, physW, physH);

        // M key: minimize mode — exit fullscreen, freeze audio, minimize
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.M) {
                AudioManager.getInstance().pauseAllLoops();
                SceneManager.getInstance().pauseIfPlaying();
                // Must exit fullscreen before iconifying on macOS
                stage.setFullScreen(false);
                // Defer iconify one pulse so the fullscreen exit can complete
                javafx.application.Platform.runLater(() -> {
                    stage.setIconified(true);
                });
                e.consume();
            }
        });

        // Restore from minimize → re-apply fullscreen + game stays paused (already done above)
        stage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (!isIconified) {
                javafx.application.Platform.runLater(() -> stage.setFullScreen(true));
            }
        });

        SceneManager.getInstance().showMainMenu();
    }

    /** Apply window mode: always fullscreen regardless of resolution setting (letterboxing is handled by GameRenderer). */
    public static void applyResolution(Stage stage, GameConfig cfg, double physW, double physH) {
        stage.setFullScreen(true);
    }
}
