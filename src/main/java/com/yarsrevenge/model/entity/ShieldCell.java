package com.yarsrevenge.model.entity;

import javafx.scene.paint.Color;

public class ShieldCell {

    public static final double CELL_W = 25.0;
    public static final double CELL_H;

    static {
        // Cells evenly fill SHIELD_HEIGHT / shieldRows; computed dynamically
        CELL_H = 44.0; // default, overridden per-wave
    }

    public final int col;
    public final int row;
    public double x;
    public double y;
    public double cellWidth;
    public double cellHeight;
    public boolean alive = true;
    public Color color;

    public ShieldCell(int col, int row, double x, double y, double cellWidth, double cellHeight, Color color) {
        this.col = col;
        this.row = row;
        this.x = x;
        this.y = y;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.color = color;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + cellWidth && py >= y && py <= y + cellHeight;
    }

    public boolean intersects(double rx, double ry, double rw, double rh) {
        return rx < x + cellWidth && rx + rw > x && ry < y + cellHeight && ry + rh > y;
    }
}
