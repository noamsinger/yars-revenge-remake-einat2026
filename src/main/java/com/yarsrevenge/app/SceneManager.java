package com.yarsrevenge.app;

import com.yarsrevenge.screen.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static SceneManager instance;

    private final Stage stage;
    private final Scene scene;
    private final double physW;
    private final double physH;

    private Screen activeScreen;

    private SceneManager(Stage stage, Scene scene, double physW, double physH) {
        this.stage = stage;
        this.scene = scene;
        this.physW = physW;
        this.physH = physH;
    }

    public static void init(Stage stage, Scene scene, double physW, double physH) {
        instance = new SceneManager(stage, scene, physW, physH);
    }

    public static SceneManager getInstance() {
        return instance;
    }

    public double getPhysW() { return physW; }
    public double getPhysH() { return physH; }
    public Scene getScene()  { return scene; }
    public Stage getStage()  { return stage; }

    public void switchTo(Screen next) {
        if (activeScreen != null) activeScreen.hide();
        scene.setRoot((Parent) next.getRoot());
        activeScreen = next;
        next.show();
    }

    public void showMainMenu() {
        switchTo(new MainMenuScreen());
    }

    public void showGame() {
        switchTo(new GameScreen());
    }

    public void showGameOver(int score, int wave) {
        switchTo(new MainMenuScreen(score, wave));
    }

    public void showHighScores() {
        switchTo(new HighScoreScreen());
    }

    public void showConfig() {
        switchTo(new ConfigScreen());
    }

    public void showInstructions() {
        switchTo(new InstructionsScreen());
    }

    public void showAbout() {
        switchTo(new AboutScreen());
    }

    /** If a GameScreen is currently active, put it in PAUSED state. */
    public void pauseIfPlaying() {
        if (activeScreen instanceof GameScreen gs) {
            gs.pause();
        }
    }
}
