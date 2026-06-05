package com.yarsrevenge.screen;

import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.app.YarsRevengeApp;
import com.yarsrevenge.audio.AudioManager;
import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.renderer.GameFont;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

public class ConfigScreen implements com.yarsrevenge.screen.Screen {

    // Row indices
    private static final int ROW_AUDIO = 0;
    private static final int ROW_FPS   = 1;
    private static final int ROW_RES   = 2;
    private static final int ROW_BACK  = 3;
    private static final int NUM_ROWS  = 4;

    private static final String[] ROW_LABELS = {"AUDIO", "FPS", "RESOLUTION", "BACK TO MENU"};

    // Options per config row (ROW_BACK has none)
    private static final String[][] ROW_OPTIONS = {
        {"ON", "OFF"},
        {"6", "15", "30"},
        {"FIT", "1024x576", "1920x1080", "3840x2160"},
    };

    private static final Image BG_IMAGE = new Image(
        ConfigScreen.class.getResourceAsStream("/background.png"));

    private final StackPane root;
    private final Canvas    canvas;
    private AnimationTimer  animTimer;
    private long            startNanos = -1;

    private int    selectedRow = 0;
    private final int[] selectedCol = new int[NUM_ROWS];

    public ConfigScreen() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        root   = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");
    }

    @Override
    public void show() {
        GameConfig cfg = GameConfig.getInstance();
        selectedCol[ROW_AUDIO] = cfg.isAudioEnabled() ? 0 : 1;
        selectedCol[ROW_FPS]   = indexOfFps(cfg.getFpsLimit());
        selectedCol[ROW_RES]   = indexOfRes(cfg.getResolution());
        selectedCol[ROW_BACK]  = 0;

        SceneManager.getInstance().getScene().setOnKeyPressed(e -> {
            KeyCode key = e.getCode();
            if (key == KeyCode.ESCAPE) {
                SceneManager.getInstance().showMainMenu();
            } else if (key == KeyCode.UP || key == KeyCode.W) {
                selectedRow = (selectedRow - 1 + NUM_ROWS) % NUM_ROWS;
            } else if (key == KeyCode.DOWN || key == KeyCode.S) {
                selectedRow = (selectedRow + 1) % NUM_ROWS;
            } else if (key == KeyCode.LEFT) {
                if (selectedRow != ROW_BACK) cycleOption(-1);
            } else if (key == KeyCode.RIGHT) {
                if (selectedRow != ROW_BACK) cycleOption(+1);
            } else if (key == KeyCode.ENTER || key == KeyCode.SPACE) {
                if (selectedRow == ROW_BACK) SceneManager.getInstance().showMainMenu();
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

    private void cycleOption(int dir) {
        String[] opts = ROW_OPTIONS[selectedRow];
        int cur = selectedCol[selectedRow];
        for (int step = 1; step <= opts.length; step++) {
            int next = ((cur + dir * step) % opts.length + opts.length) % opts.length;
            if (isUnavailable(selectedRow, opts[next])) continue;
            selectedCol[selectedRow] = next;
            break;
        }
        applyConfig();
    }

    private boolean isUnavailable(int row, String opt) {
        if (row != ROW_RES) return false;
        Rectangle2D b = Screen.getPrimary().getBounds();
        return switch (opt) {
            case "1920x1080" -> b.getWidth() < 1920 || b.getHeight() < 1080;
            case "3840x2160" -> b.getWidth() < 3840 || b.getHeight() < 2160;
            default          -> false;
        };
    }

    private void applyConfig() {
        GameConfig cfg = GameConfig.getInstance();
        cfg.setAudioEnabled(selectedCol[ROW_AUDIO] == 0);
        cfg.setFpsLimit(Integer.parseInt(ROW_OPTIONS[ROW_FPS][selectedCol[ROW_FPS]]));
        cfg.setResolution(ROW_OPTIONS[ROW_RES][selectedCol[ROW_RES]]);
        AudioManager.getInstance().setMuted(!cfg.isAudioEnabled());
        YarsRevengeApp.applyResolution(
            SceneManager.getInstance().getStage(), cfg,
            Screen.getPrimary().getBounds().getWidth(),
            Screen.getPrimary().getBounds().getHeight());
    }

    private static int indexOfFps(int fps) {
        return switch (fps) { case 6 -> 0; case 15 -> 1; default -> 2; };
    }

    private static int indexOfRes(String res) {
        return switch (res) {
            case "1024x576"  -> 1;
            case "1920x1080" -> 2;
            case "3840x2160" -> 3;
            default          -> 0;
        };
    }

    // -----------------------------------------------------------------------
    // Drawing
    // -----------------------------------------------------------------------

    // Layout constants (in logical 1920×1080 coords)
    private static final double LX      = 80;    // left edge of glass panel
    private static final double RX      = 1840;  // right edge of glass panel
    private static final double PW      = RX - LX;
    private static final double LABEL_X = 130;   // label text left
    private static final double OPT_X   = 620;   // options area left
    private static final double OPT_W   = RX - OPT_X - 50; // options area width
    private static final double ROW_H   = 130;   // pixels per row
    private static final double ROWS_TOP = 180;  // y of first row top

    private void draw(double t) {
        double physW = canvas.getWidth();
        double physH = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, physW, physH);
        gc.save();
        gc.scale(physW / GameConstants.LOGICAL_W, physH / GameConstants.LOGICAL_H);

        // Background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);
        if (BG_IMAGE != null && !BG_IMAGE.isError())
            gc.drawImage(BG_IMAGE, 0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

        // Title panel
        glassRect(gc, LX, 28, PW, 105);
        double pulse = 0.75 + 0.25 * Math.sin(t * 2.2);
        gc.setFont(GameFont.of(46));
        gc.setFill(Color.rgb((int)(255 * pulse), (int)(220 * pulse), 50));
        drawCentred(gc, "CONFIGURATION", GameConstants.LOGICAL_W / 2.0, 100);

        // Rows panel
        double panelH = NUM_ROWS * ROW_H + 20;
        glassRect(gc, LX, ROWS_TOP - 15, PW, panelH);

        for (int row = 0; row < NUM_ROWS; row++) {
            double cy = ROWS_TOP + row * ROW_H + ROW_H * 0.58; // baseline
            boolean sel = (row == selectedRow);
            double p = sel ? (0.78 + 0.22 * Math.sin(t * 5)) : 1.0;

            // Cursor + label
            gc.setFont(GameFont.of(26));
            gc.setFill(sel ? Color.rgb((int)(255*p),(int)(255*p),50) : Color.rgb(180,180,180));
            gc.fillText((sel ? "> " : "  ") + ROW_LABELS[row], LABEL_X, cy);

            if (row == ROW_BACK) continue; // no options for BACK row

            // Options — evenly spread across OPT_W
            String[] opts = ROW_OPTIONS[row];
            double spacing = OPT_W / opts.length;
            gc.setFont(GameFont.of(24));
            for (int col = 0; col < opts.length; col++) {
                boolean optSel = (selectedCol[row] == col);
                boolean na     = isUnavailable(row, opts[col]);
                String  label  = opts[col] + (na ? "(N/A)" : "");

                Color c;
                if (na)             c = Color.rgb(70, 70, 70);
                else if (optSel && sel) c = Color.rgb((int)(255*p),(int)(255*p),50);
                else if (optSel)    c = Color.rgb(200, 200, 80);
                else                c = Color.rgb(120, 120, 120);

                gc.setFill(c);
                // Centre each option within its slot
                double slotCx = OPT_X + col * spacing + spacing / 2.0;
                drawCentred(gc, label, slotCx, cy);
            }
        }

        // Footer
        gc.setFont(GameFont.of(14));
        gc.setFill(Color.rgb(100, 100, 100));
        drawCentred(gc, "UP/DOWN: ROW   LEFT/RIGHT: CHANGE   ENTER: BACK TO MENU   ESC: BACK",
                    GameConstants.LOGICAL_W / 2.0, GameConstants.LOGICAL_H - 30);

        gc.restore();
    }

    private void glassRect(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.rgb(0, 0, 0, 0.62));
        gc.fillRoundRect(x, y, w, h, 16, 16);
        gc.setStroke(Color.rgb(255, 255, 255, 0.09));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 16, 16);
    }

    /** Approximate centred text — Press Start 2P is monospaced, ~0.62 * fontSize per char. */
    private void drawCentred(GraphicsContext gc, String text, double cx, double y) {
        double approxW = text.length() * gc.getFont().getSize() * 0.62;
        gc.fillText(text, cx - approxW / 2.0, y);
    }
}
