package com.yarsrevenge.renderer;

import com.yarsrevenge.model.entity.ShieldCell;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ShieldRenderer implements Renderer {

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        for (ShieldCell cell : ctx.shield().getCells()) {
            if (!cell.alive) continue;
            gc.setFill(cell.color);
            gc.fillRect(cell.x, cell.y, cell.cellWidth - 1, cell.cellHeight - 1);
            // Bright border for retro look
            gc.setStroke(cell.color.brighter());
            gc.setLineWidth(1.0);
            gc.strokeRect(cell.x, cell.y, cell.cellWidth - 1, cell.cellHeight - 1);
        }
    }
}
