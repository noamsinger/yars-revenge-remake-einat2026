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

    // Polar coordinates relative to Quotile center, fixed at build time
    private final List<Double> cellRadius = new ArrayList<>();
    private final List<Double> cellAngle  = new ArrayList<>();

    private static final Random RNG = new Random(0xBADC0DE);

    private static final double MIN_RADIUS =  80.0;
    private static final double MAX_RADIUS = 350.0;

    // Rotation state (odd waves only)
    private double rotationAngle = 0.0;
    private static final double ROTATION_SPEED = Math.toRadians(40.0); // 40 deg/sec

    // Scrolling state (even waves only)
    private int    scrollOffset = 0;
    private double scrollTimer  = 0.0;
    private static final double SCROLL_INTERVAL = 0.1; // seconds per slot advance

    private boolean rectMode = false;

    // Rect-mode slot positions (fixed, computed once in rebuildRect)
    private final List<Double> slotX = new ArrayList<>();
    private final List<Double> slotY = new ArrayList<>();

    public Shield(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        rebuild(rows);
    }

    public void rebuild(int shieldRows) {
        rectMode = false;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        rotationAngle = 0.0;
        scrollOffset  = 0;
        scrollTimer   = 0.0;

        double cw = ShieldCell.CELL_W;

        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double qcy = GameConstants.LOGICAL_H / 2.0;

        int halfSteps = (int) Math.ceil(MAX_RADIUS / cw) + 1;

        RNG.setSeed(0xBADC0DE);

        for (int drow = -halfSteps; drow <= halfSteps; drow++) {
            for (int dcol = -halfSteps; dcol <= 1; dcol++) {
                double offX = dcol * cw + cw / 2.0;
                double offY = drow * cw + cw / 2.0;

                double dist = Math.sqrt(offX * offX + offY * offY);
                if (dist < MIN_RADIUS || dist > MAX_RADIUS) continue;

                double cx = qcx + offX;
                double cy = qcy + offY;

                if (cx - cw / 2.0 < 0 || cx + cw / 2.0 > GameConstants.LOGICAL_W) continue;
                if (cy - cw / 2.0 < 0 || cy + cw / 2.0 > GameConstants.LOGICAL_H) continue;

                int col = (int)(cx / cw);
                int row = (int)(cy / cw);
                int red = 0x60 + RNG.nextInt(0x61);
                Color color = Color.rgb(red, 0, 0);
                cells.add(new ShieldCell(col, row,
                    cx - cw / 2.0, cy - cw / 2.0, cw, cw, color));
                cellRadius.add(dist);
                cellAngle.add(Math.atan2(offY, offX));
            }
        }
    }

    /** Flat rectangular wall for even waves. */
    public void rebuildRect(int shieldRows) {
        rectMode = true;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        rotationAngle = 0.0;
        scrollOffset  = 0;
        scrollTimer   = 0.0;

        double cw       = ShieldCell.CELL_W;
        int    rectCols = (int) Math.round(GameConstants.SHIELD_COLS * 1.3);
        int    rectRows = (int) Math.round(shieldRows * 1.2);

        // Center the wall on LOGICAL_H/2 (updateScrolling will shift it with Quotile Y)
        double totalH  = rectRows * cw;
        double baseY   = GameConstants.LOGICAL_H / 2.0 - totalH / 2.0;

        RNG.setSeed(0xBADC0DE);

        // Build slots left→right, top→bottom so scrolling reads naturally
        for (int r = 0; r < rectRows; r++) {
            for (int c = 0; c < rectCols; c++) {
                double x = GameConstants.SHIELD_X + c * cw;
                double y = baseY + r * cw;
                slotX.add(x);
                slotY.add(y);
                int red = 0x60 + RNG.nextInt(0x61);
                Color color = Color.rgb(red, 0, 0);
                cells.add(new ShieldCell(c, r, x, y, cw, cw, color));
            }
        }
    }

    /** Static shield — cells track Quotile Y but don't rotate. */
    public void update(double quotileCenterY) {
        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double cw  = ShieldCell.CELL_W;
        for (int i = 0; i < cells.size(); i++) {
            ShieldCell c = cells.get(i);
            double angle = cellAngle.get(i);
            double r     = cellRadius.get(i);
            double offX  = Math.cos(angle) * r;
            double offY  = Math.sin(angle) * r;
            c.x = qcx + offX - cw / 2.0;
            c.y = quotileCenterY + offY - c.cellHeight / 2.0;
        }
    }

    /** Rotating shield — cells orbit around the Quotile, following its Y. */
    public void updateRotating(double quotileCenterY, double dt) {
        rotationAngle += ROTATION_SPEED * dt;
        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double cw  = ShieldCell.CELL_W;
        for (int i = 0; i < cells.size(); i++) {
            ShieldCell c = cells.get(i);
            double angle = cellAngle.get(i) + rotationAngle;
            double r     = cellRadius.get(i);
            double offX  = Math.cos(angle) * r;
            double offY  = Math.sin(angle) * r;
            c.x = qcx + offX - cw / 2.0;
            c.y = quotileCenterY + offY - c.cellHeight / 2.0;
        }
    }

    /**
     * Scrolling shield (even waves) — flat rect wall tracks Quotile Y, cells cycle
     * through slots in reading order (left→right, top→bottom, wrapping back to top).
     */
    public void updateScrolling(double quotileCenterY, double dt) {
        scrollTimer += dt;
        while (scrollTimer >= SCROLL_INTERVAL) {
            scrollTimer -= SCROLL_INTERVAL;
            scrollOffset = (scrollOffset + 1) % cells.size();
        }

        // Shift the whole wall so its vertical centre follows the Quotile
        double yOffset = quotileCenterY - GameConstants.LOGICAL_H / 2.0;
        int n = cells.size();
        for (int i = 0; i < n; i++) {
            int slot = (i + scrollOffset) % n;
            ShieldCell c = cells.get(i);
            c.x = slotX.get(slot);
            c.y = slotY.get(slot) + yOffset;
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

    public double getMinCellOffsetY() {
        double min = 0;
        for (int i = 0; i < cellAngle.size(); i++) {
            double oy = Math.sin(cellAngle.get(i)) * cellRadius.get(i);
            if (oy < min) min = oy;
        }
        return min;
    }

    public double getMaxCellOffsetY() {
        double max = 0;
        for (int i = 0; i < cellAngle.size(); i++) {
            double oy = Math.sin(cellAngle.get(i)) * cellRadius.get(i);
            if (oy > max) max = oy;
        }
        return max;
    }

    public List<ShieldCell> getCells() { return cells; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
