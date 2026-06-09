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

    // Rect-mode dimensions — stored for alternating-row scroll
    private int rectCols = 0;
    private int rectRows = 0;

    // Rect-mode slot positions (fixed, computed once in rebuildRect)
    private final List<Double> slotX = new ArrayList<>();
    private final List<Double> slotY = new ArrayList<>();

    // Swarm drift velocities (one per cell, set in rebuildSwarm)
    private final List<Double> swarmVx = new ArrayList<>();
    private final List<Double> swarmVy = new ArrayList<>();

    public Shield(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        rebuild(rows);
    }

    public void rebuild(int shieldRows) {
        rectMode = false;
        rectCols = 0; rectRows = 0;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        swarmVx.clear();
        swarmVy.clear();
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

    /** Flat rectangular wall — CYCLING_FENCE mode. */
    public void rebuildRect(int shieldRows) {
        rectMode = true;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        swarmVx.clear();
        swarmVy.clear();
        rotationAngle = 0.0;
        scrollOffset  = 0;
        scrollTimer   = 0.0;

        double cw = ShieldCell.CELL_W;
        rectCols = (int) Math.round(GameConstants.SHIELD_COLS * 1.3);
        rectRows = (int) Math.round(shieldRows * 1.2);

        double totalH = rectRows * cw;
        double baseY  = GameConstants.LOGICAL_H / 2.0 - totalH / 2.0;

        RNG.setSeed(0xBADC0DE);

        for (int r = 0; r < rectRows; r++) {
            for (int c = 0; c < rectCols; c++) {
                double x = GameConstants.SHIELD_X + c * cw;
                double y = baseY + r * cw;
                slotX.add(x);
                slotY.add(y);
                int red = 0x60 + RNG.nextInt(0x61);
                cells.add(new ShieldCell(c, r, x, y, cw, cw, Color.rgb(red, 0, 0)));
            }
        }
    }

    /** Same arc geometry as ARCH_BARRICADE but used for ROTATING_CIRCLE. */
    public void rebuildRotating(int shieldRows) {
        rectMode = false;
        rectCols = 0; rectRows = 0;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        swarmVx.clear();
        swarmVy.clear();
        rotationAngle = 0.0;
        scrollOffset  = 0;
        scrollTimer   = 0.0;

        double cw  = ShieldCell.CELL_W;
        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double qcy = GameConstants.LOGICAL_H / 2.0;

        int halfSteps = (int) Math.ceil(MAX_RADIUS / cw) + 1;
        RNG.setSeed(0xBADC0DE);

        // Full circle — no dcol limit, no screen-edge clipping (cells orbit freely)
        for (int drow = -halfSteps; drow <= halfSteps; drow++) {
            for (int dcol = -halfSteps; dcol <= halfSteps; dcol++) {
                double offX = dcol * cw + cw / 2.0;
                double offY = drow * cw + cw / 2.0;
                double dist = Math.sqrt(offX * offX + offY * offY);
                if (dist < MIN_RADIUS || dist > MAX_RADIUS) continue;

                int red = 0x60 + RNG.nextInt(0x61);
                cells.add(new ShieldCell(dcol, drow,
                    qcx + offX - cw / 2.0, qcy + offY - cw / 2.0, cw, cw,
                    Color.rgb(red, 0, 0)));
                cellRadius.add(dist);
                cellAngle.add(Math.atan2(offY, offX));
            }
        }
    }

    /** Random cloud of cells spread in the annular arc zone — RANDOM_SWARM. */
    public void rebuildSwarm(int shieldRows) {
        rectMode = false;
        rectCols = 0; rectRows = 0;
        cells.clear();
        cellRadius.clear();
        cellAngle.clear();
        slotX.clear();
        slotY.clear();
        swarmVx.clear();
        swarmVy.clear();
        rotationAngle = 0.0;
        scrollOffset  = 0;
        scrollTimer   = 0.0;

        double cw    = ShieldCell.CELL_W;
        int    count = GameConstants.SHIELD_COLS * shieldRows * 3;
        double qcx   = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double qcy   = GameConstants.LOGICAL_H / 2.0;

        RNG.setSeed(0xDEADBEEF);

        for (int i = 0; i < count; i++) {
            // Random polar coords within the annular zone, left half only (angle 90°..270°)
            double angle = Math.PI / 2.0 + RNG.nextDouble() * Math.PI;
            double r     = MIN_RADIUS + RNG.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
            double offX  = Math.cos(angle) * r;
            double offY  = Math.sin(angle) * r;
            // Slow angular drift
            double dAngle = (RNG.nextDouble() - 0.5) * Math.toRadians(20.0);
            double dR     = (RNG.nextDouble() - 0.5) * 15.0;
            int red = 0x60 + RNG.nextInt(0x61);
            cells.add(new ShieldCell(i, 0, qcx + offX - cw / 2.0, qcy + offY - cw / 2.0, cw, cw, Color.rgb(red, 0, 0)));
            cellAngle.add(angle);
            cellRadius.add(r);
            swarmVx.add(dAngle); // angular drift (rad/s)
            swarmVy.add(dR);     // radial drift (px/s)
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
     * CYCLING_FENCE: cells move along a snaking path through all slots.
     * Row 0 (odd, 1-indexed) goes L→R, row 1 goes R→L, alternating.
     * Each cell position advances one slot per tick, wrapping to the start
     * of the next row when it reaches the end — creating a continuous conveyor.
     */
    public void updateScrolling(double quotileCenterY, double dt) {
        scrollTimer += dt;
        while (scrollTimer >= SCROLL_INTERVAL) {
            scrollTimer -= SCROLL_INTERVAL;
            scrollOffset++;
        }

        double yOffset = quotileCenterY - GameConstants.LOGICAL_H / 2.0;
        int total = rectCols * rectRows;
        int n = cells.size();

        for (int i = 0; i < n; i++) {
            ShieldCell c = cells.get(i);
            // Linear position of this cell in the snake path, advanced by scrollOffset
            int snakePos = (i + scrollOffset) % total;
            // Which row and column in the snake
            int snakeRow = snakePos / rectCols;
            int snakeCol = snakePos % rectCols;
            // Odd rows (0-indexed even → 1-indexed odd) go L→R; even rows go R→L
            int col = (snakeRow % 2 == 0) ? snakeCol : (rectCols - 1 - snakeCol);
            int slotIdx = snakeRow * rectCols + col;
            c.x = slotX.get(slotIdx);
            c.y = slotY.get(slotIdx) + yOffset;
        }
    }

    /** RANDOM_SWARM: cells drift in polar coords, following Quotile Y, bouncing off arc bounds. */
    public void updateSwarm(double quotileCenterY, double dt) {
        double qcx = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
        double cw  = ShieldCell.CELL_W;

        for (int i = 0; i < cells.size(); i++) {
            ShieldCell c = cells.get(i);
            double angle  = cellAngle.get(i)  + swarmVx.get(i) * dt;
            double r      = cellRadius.get(i) + swarmVy.get(i) * dt;

            // Keep angle in left half (90°..270°) by bouncing
            double lo = Math.PI / 2.0, hi = 3.0 * Math.PI / 2.0;
            if (angle < lo) { angle = lo + (lo - angle); swarmVx.set(i, -swarmVx.get(i)); }
            if (angle > hi) { angle = hi - (angle - hi); swarmVx.set(i, -swarmVx.get(i)); }

            // Bounce off MIN/MAX radius
            if (r < MIN_RADIUS) { r = MIN_RADIUS + (MIN_RADIUS - r); swarmVy.set(i, -swarmVy.get(i)); }
            if (r > MAX_RADIUS) { r = MAX_RADIUS - (r - MAX_RADIUS); swarmVy.set(i, -swarmVy.get(i)); }

            cellAngle.set(i, angle);
            cellRadius.set(i, r);

            c.x = qcx + Math.cos(angle) * r - cw / 2.0;
            c.y = quotileCenterY + Math.sin(angle) * r - cw / 2.0;
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
    public boolean isRectMode() { return rectMode; }

    /** Half the total height of the rect wall — used to clamp Quotile Y on even waves. */
    public double getRectHalfHeight() {
        int rectRows = (int) Math.round(rows * 1.2);
        return rectRows * ShieldCell.CELL_W / 2.0;
    }
}
