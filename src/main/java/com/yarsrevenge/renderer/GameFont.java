package com.yarsrevenge.renderer;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameFont {

    private static final Logger log = LoggerFactory.getLogger(GameFont.class);
    private static Font loaded = null;
    private static final String FAMILY = "Press Start 2P";

    static {
        try {
            Font.loadFont(GameFont.class.getResourceAsStream("/PressStart2P-Regular.ttf"), 12);
        } catch (Exception e) {
            log.error("Could not load Press Start 2P font", e);
        }
    }

    private GameFont() {}

    public static Font of(double size) {
        return Font.font(FAMILY, size);
    }

    public static Font bold(double size) {
        // Press Start 2P has no bold variant — regular looks bold already
        return Font.font(FAMILY, FontWeight.BOLD, size);
    }
}
