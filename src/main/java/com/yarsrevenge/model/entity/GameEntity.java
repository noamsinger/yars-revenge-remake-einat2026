package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameState;
import javafx.geometry.Rectangle2D;

public abstract class GameEntity {

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected boolean alive = true;

    public GameEntity(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(double dt, GameState state);

    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    public boolean intersects(GameEntity other) {
        return getBounds().intersects(other.getBounds());
    }

    public boolean isAlive() { return alive; }
    public void kill()       { alive = false; }

    public double getX()       { return x; }
    public double getY()       { return y; }
    public double getWidth()   { return width; }
    public double getHeight()  { return height; }
    public double getCenterX() { return x + width / 2.0; }
    public double getCenterY() { return y + height / 2.0; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}
