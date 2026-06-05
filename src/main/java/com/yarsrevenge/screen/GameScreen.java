package com.yarsrevenge.screen;

import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.app.YarsRevengeApp;
import com.yarsrevenge.audio.AudioManager;
import com.yarsrevenge.audio.SoundEffect;
import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.controller.GameController;
import com.yarsrevenge.controller.InputHandler;
import com.yarsrevenge.engine.GameLoop;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.renderer.GameRenderer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;

public class GameScreen implements com.yarsrevenge.screen.Screen {

    private final StackPane root;
    private final Canvas canvas;
    private final double physW;
    private final double physH;
    // Letterbox draw region within the full canvas
    private final double drawX;
    private final double drawY;
    private final double drawW;
    private final double drawH;

    private GameState state;
    private GameLoop gameLoop;
    private InputHandler inputHandler;

    public GameScreen() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        physW = bounds.getWidth();
        physH = bounds.getHeight();

        // Compute letterbox region from config
        GameConfig cfg = GameConfig.getInstance();
        int cfgW = cfg.getResolutionWidth();
        int cfgH = cfg.getResolutionHeight();
        if (cfgW > 0 && cfgH > 0) {
            // Center the requested resolution within the full screen
            drawW = Math.min(cfgW, physW);
            drawH = Math.min(cfgH, physH);
            drawX = Math.floor((physW - drawW) / 2.0);
            drawY = Math.floor((physH - drawH) / 2.0);
        } else {
            // FIT — fill entire screen
            drawX = 0; drawY = 0; drawW = physW; drawH = physH;
        }

        canvas = new Canvas(physW, physH);
        root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    @Override
    public void show() {
        state = new GameState();

        GameRenderer renderer = new GameRenderer();
        renderer.setPhysicalSize(physW, physH);
        renderer.setDrawRegion(drawX, drawY, drawW, drawH);

        inputHandler = new InputHandler();
        inputHandler.attach(SceneManager.getInstance().getScene());

        GameController controller = new GameController(inputHandler);

        gameLoop = new GameLoop(state, controller, renderer, canvas.getGraphicsContext2D());
        gameLoop.start();

        AudioManager.getInstance().startBgHum();
        AudioManager.getInstance().play(SoundEffect.WAVE_START);
    }

    @Override
    public void hide() {
        if (gameLoop != null) gameLoop.stop();
        if (inputHandler != null) inputHandler.detach(SceneManager.getInstance().getScene());
        AudioManager.getInstance().stopBgHum();
        AudioManager.getInstance().stopCannonFly();
    }

    /** Called when the window is restored from minimize — freeze game and show pause overlay. */
    public void pause() {
        if (state != null && state.getPhase() == GameState.Phase.PLAYING) {
            state.setPhase(GameState.Phase.PAUSED);
            state.queueAudio(com.yarsrevenge.model.GameState.AudioEvent.STOP_ALL_LOOPS);
        }
    }

    @Override
    public Node getRoot() { return root; }
}
