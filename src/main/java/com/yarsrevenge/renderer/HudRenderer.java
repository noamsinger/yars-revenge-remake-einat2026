package com.yarsrevenge.renderer;

import com.yarsrevenge.model.GameConstants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class HudRenderer implements Renderer {

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        gc.setFont(GameFont.of(20));
        gc.setFill(Color.WHITE);
        gc.fillText("SCORE: " + ctx.score(), 30, 36);
        gc.fillText("WAVE: "  + ctx.wave(),  30, 68);
        gc.fillText("LIVES: " + ctx.lives(), 30, 100);

        gc.setFont(GameFont.of(14));
        if (ctx.playerInNZ()) {
            gc.setFill(Color.rgb(100, 220, 255));
            gc.fillText("NZ: MISSILES BLOCKED | CANNON DISABLED",
                        250, GameConstants.LOGICAL_H - 20);
        } else if (ctx.cannonCharged()) {
            gc.setFill(Color.rgb(255, 255, 80));
            gc.fillText("SPACE:BULLET  ENTER:CANNON  ESC:PAUSE",
                        30, GameConstants.LOGICAL_H - 20);
        } else {
            gc.setFill(Color.rgb(120, 120, 120));
            gc.fillText("SPACE:BULLET  ENTER:CANNON(CHARGE!)  ESC:PAUSE",
                        30, GameConstants.LOGICAL_H - 20);
        }

        if (ctx.debugMode()) {
            gc.setFont(GameFont.of(16));
            gc.setFill(Color.rgb(255, 80, 80));
            gc.fillText("** DEBUG MODE **", 30, GameConstants.LOGICAL_H - 55);
        }
    }
}
