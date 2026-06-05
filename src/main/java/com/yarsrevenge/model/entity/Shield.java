package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shield {

    private final List<ShieldCell> cells = new ArrayList<>();
    private final int cols;
    private final int rows;

    private final List<Double> cellOffsetX = new ArrayList<>();
    private final List<Double> cellOffsetY = new ArrayList<>();

    private static final Random RNG = new Random(0xBADC0DE);

    // Distance ring parameters (in logical pixels from Quotile center)
    private static final double MIN_RADIUS =  80.0;
    private static final double MAX_RADIUS = 350.0;

    public Shield(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        rebuild(rows);
    }

    /**
     * Tiles the area around the Quotile with CELL_W × CELL_W cells and keeps
     * only those whose center is within [MIN_RADIUS, MAX_RADIUS] of the Quotile
     * center. The grid is centered on the Quotile so the pattern is symmetric
     * in Y. Cells outside screen bounds are dropped.
     */
    public void rebuild(int shieldRows) {
        cells.clear();
        cellOffsetX.clear();
        cellOffsetY.clear();

        double cw = ShieldCell.CELL_W;

        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double qcy = GameConstants.LOGICAL_H / 2.0;

        // Iterate offsets from the Quotile center, aligned to a cw grid
        int halfSteps = (int) Math.ceil(MAX_RADIUS / cw) + 1;

        RNG.setSeed(0xBADC0DE);

        for (int drow = -halfSteps; drow <= halfSteps; drow++) {
            for (int dcol = -halfSteps; dcol <= 1; dcol++) { // dcol<=1: only left side + Quotile col
                double offX = dcol * cw + cw / 2.0;  // offset from Quotile center
                double offY = drow * cw + cw / 2.0;

                double dist = Math.sqrt(offX * offX + offY * offY);
                if (dist < MIN_RADIUS || dist > MAX_RADIUS) continue;

                double cx = qcx + offX;
                double cy = qcy + offY;

                // Screen bounds check
                if (cx - cw / 2.0 < 0 || cx + cw / 2.0 > GameConstants.LOGICAL_W) continue;
                if (cy - cw / 2.0 < 0 || cy + cw / 2.0 > GameConstants.LOGICAL_H) continue;

                int col = (int)(cx / cw);
                int row = (int)(cy / cw);
                int red = 0x60 + RNG.nextInt(0x61);
                Color color = Color.rgb(red, 0, 0);
                cells.add(new ShieldCell(col, row,
                    cx - cw / 2.0, cy - cw / 2.0, cw, cw, color));
                cellOffsetX.add(offX);
                cellOffsetY.add(offY);
            }
        }
    }

    /** Repositions all cells to follow the Quotile's center. */
    public void update(double quotileCenterY) {
        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double cw  = ShieldCell.CELL_W;
        for (int i = 0; i < cells.size(); i++) {
            ShieldCell c = cells.get(i);
            double cx = qcx + cellOffsetX.get(i);
            double cy = quotileCenterY + cellOffsetY.get(i);
            c.x = cx - cw / 2.0;
            c.y = cy - c.cellHeight / 2.0;
        }
    }

    public ShieldCell eatAt(double px, double py, double pw, double ph) {
        for (ShieldCell cell : cells) {
            if (cell.alive && cell.intersects(px, py, pw, ph)) {
                cell.alive = false;
                return cell;
            }
        }
        return null;
    }

    public ShieldCell destroyAt(double px, double py, double pw, double ph) {
        for (ShieldCell cell : cells) {
            if (cell.alive && cell.intersects(px, py, pw, ph)) {
                cell.alive = false;
                return cell;
            }
        }
        return null;
    }

    public boolean isColumnClearAt(double quotileX) {
        for (ShieldCell cell : cells) {
            if (cell.alive && cell.col == 0) return false;
        }
        return true;
    }

    public boolean allDestroyed() {
        for (ShieldCell cell : cells) {
            if (cell.alive) return false;
        }
        return true;
    }

    /** Min and max Y offsets of any cell relative to the Quotile center. */
    public double getMinCellOffsetY() {
        double min = 0;
        for (double oy : cellOffsetY) if (oy < min) min = oy;
        return min;
    }

    public double getMaxCellOffsetY() {
        double max = 0;
        for (double oy : cellOffsetY) if (oy > max) max = oy;
        return max;
    }

    public List<ShieldCell> getCells() { return cells; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
