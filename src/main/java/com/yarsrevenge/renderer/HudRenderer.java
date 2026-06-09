package com.yarsrevenge.renderer;

import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.wave.WaveConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class HudRenderer implements Renderer {

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        gc.setFont(GameFont.of(20));
        gc.setFill(Color.WHITE);
        gc.fillText("EXP: " + ctx.score(), 30, 36);
        gc.fillText("WAVE: "  + ctx.wave(),  30, 68);
        gc.fillText("LIVES: " + ctx.lives(), 30, 100);

        // Ultimate mode: show Tron counter
        if (ctx.gameMode() == GameConfig.GameMode.ULTIMATE) {
            int trons = ctx.tronCount();
            gc.setFont(GameFont.of(18));
            gc.setFill(trons >= 5 ? Color.rgb(100, 255, 100) : Color.rgb(255, 200, 50));
            gc.fillText("TRONS: " + trons + "/5", 30, 132);
        }

        gc.setFont(GameFont.of(14));
        drawBottomBar(gc, ctx);

        if (ctx.autoPilot()) {
            gc.setFont(GameFont.of(18));
            gc.setFill(Color.rgb(80, 255, 160));
            gc.fillText("** AUTOPILOT **", GameConstants.LOGICAL_W - 320, 36);
        }

        if (ctx.debugMode()) {
            drawDebugStats(gc, ctx);
        }
    }

    private void drawBottomBar(GraphicsContext gc, RenderContext ctx) {
        GameConfig.GameMode mode = ctx.gameMode();
        if (ctx.playerInNZ()) {
            gc.setFill(Color.rgb(100, 220, 255));
            gc.fillText("NZ: MISSILES BLOCKED | CANNON DISABLED",
                        250, GameConstants.LOGICAL_H - 20);
        } else if (mode == GameConfig.GameMode.ULTIMATE) {
            int trons = ctx.tronCount();
            if (trons < 5) {
                gc.setFill(Color.rgb(200, 160, 50));
                gc.fillText("COLLECT TRONS (" + trons + "/5): NIBBLE SHIELD OR TOUCH QUOTILE",
                            250, GameConstants.LOGICAL_H - 20);
            } else {
                gc.setFill(Color.rgb(100, 255, 100));
                gc.fillText("5 TRONS READY! FLY TO LEFT EDGE THEN ENTER:CANNON",
                            250, GameConstants.LOGICAL_H - 20);
            }
        } else if (ctx.cannonCharged()) {
            gc.setFill(Color.rgb(255, 255, 80));
            gc.fillText("SPACE:BULLET  ENTER:CANNON  ESC:PAUSE",
                        30, GameConstants.LOGICAL_H - 20);
        } else {
            gc.setFill(Color.rgb(120, 120, 120));
            gc.fillText("SPACE:BULLET  ENTER:CANNON(CHARGE!)  ESC:PAUSE",
                        30, GameConstants.LOGICAL_H - 20);
        }
    }

    private void drawDebugStats(GraphicsContext gc, RenderContext ctx) {
        WaveConfig wc = ctx.waveConfig();
        gc.setFont(GameFont.of(14));
        gc.setFill(Color.rgb(255, 80, 80));
        gc.fillText("** DEBUG **", 30, GameConstants.LOGICAL_H - 55);

        if (wc != null) {
            String shieldType = wc.shieldMode().name();
            String stats = String.format(
                "W%d | MODE:%s | SHIELD:%s | Q:%.0f | TRP:%.2f | SWIRL:%.2fx | NZ:%s | CANNON:%s",
                ctx.wave(),
                ctx.gameMode().name(),
                shieldType,
                wc.quotileSpeed(),
                wc.torpedoSpeed(),
                wc.missileSpeedMultiplier(),
                ctx.playerInNZ() ? "ON" : "off",
                ctx.cannonCharged() ? "CHARGED" : (ctx.zorlonCannon() != null ? "FLYING" : "empty")
            );
            gc.setFill(Color.rgb(200, 200, 200));
            gc.fillText(stats, 160, GameConstants.LOGICAL_H - 55);
        }
    }
}
