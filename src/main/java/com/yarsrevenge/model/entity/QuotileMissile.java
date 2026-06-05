package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class QuotileMissile extends GameEntity {

    public static final double SIZE  = GameConstants.QUOTILE_W; // same size as enemy
    public static final double SPEED = 1400.0;

    private final double vx;
    private final double vy;
    private final double angle; // radians, for sprite rotation

    public QuotileMissile(double startX, double startY, double targetX, double targetY) {
        super(startX - SIZE / 2.0, startY - SIZE / 2.0, SIZE, SIZE);
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1.0) dist = 1.0;
        this.vx    = (dx / dist) * SPEED;
        this.vy    = (dy / dist) * SPEED;
        this.angle = Math.atan2(dy, dx);
    }

    @Override
    public void update(double dt, GameState state) {
        x += vx * dt;
        y += vy * dt;

        if (x + width  < -100 || x > GameConstants.LOGICAL_W + 100 ||
            y + height < -100 || y > GameConstants.LOGICAL_H + 100) {
            alive = false;
        }
    }

    public double getAngle() { return angle; }
}
