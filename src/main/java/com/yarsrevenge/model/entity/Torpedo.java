package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

/** The Torpedo (aka Orb) — a homing energy ball that chases the player from the Quotile. */
public class Torpedo extends GameEntity {

    private static final double W = 64.0;
    private static final double H = 64.0;

    private double speed;
    private final Quotile quotile;

    public Torpedo(Quotile quotile, double speed) {
        super(quotile.getX(), quotile.getY(), W, H);
        this.quotile = quotile;
        this.speed = speed * 60;
    }

    @Override
    public void update(double dt, GameState state) {
        double targetX = state.getPlayer().getCenterX();
        double targetY = state.getPlayer().getCenterY();

        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1.0) {
            x += (dx / dist) * speed * dt;
            y += (dy / dist) * speed * dt;
        }
    }

    public void setSpeed(double angularSpeed) {
        this.speed = angularSpeed * 60;
    }

    /** Bring torpedo back to life without teleporting — used when it was just killed mid-flight. */
    public void reset() {
        this.alive = true;
    }

    /** Teleport torpedo back to Quotile origin — only called on player respawn/wave init. */
    public void respawn() {
        this.x = quotile.getX();
        this.y = quotile.getCenterY() - H / 2.0;
        this.alive = true;
    }
}
