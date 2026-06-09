package com.yarsrevenge.renderer;

import com.yarsrevenge.model.entity.ShieldCell;
import com.yarsrevenge.model.entity.ShieldMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ShieldRenderer implements Renderer {

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        int score = ctx.score();
        ShieldMode mode = ctx.waveConfig().shieldMode();
        boolean circular = mode == ShieldMode.ROTATING_CIRCLE || mode == ShieldMode.RANDOM_SWARM;

        for (ShieldCell cell : ctx.shield().getCells()) {
            if (!cell.alive) continue;
            gc.setFill(tintedColor(cell, score));
            if (circular) {
                gc.fillOval(cell.x, cell.y, cell.cellWidth - 1, cell.cellHeight - 1);
            } else {
                gc.fillRect(cell.x, cell.y, cell.cellWidth - 1, cell.cellHeight - 1);
            }
        }
    }

    private static Color tintedColor(ShieldCell cell, int score) {
        double brightness = cell.color.getRed();

        if (score >= 230_000) {
            return Color.color(brightness, brightness * 0.3, brightness * 0.5);
        } else if (score >= 150_000) {
            return Color.color(brightness * 0.7, brightness * 0.7, brightness * 0.7);
        } else if (score >= 70_000) {
            return Color.color(0, brightness * 0.4, brightness);
        } else {
            return cell.color;
        }
    }
}
