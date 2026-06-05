package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class Player extends GameEntity {

    private double vx = 0;
    private double vy = 0;

    private double facingAngle = 0.0;
    private double wingPhase = 0.0;

    private double bounceVx = 0;
    private double bounceVy = 0;
    private double bounceTimer = 0;
    private static final double BOUNCE_DURATION = 0.05;
    private static final double BOUNCE_SPEED    = 400.0;

    public Player(double x, double y) {
        super(x, y, GameConstants.PLAYER_W, GameConstants.PLAYER_H);
    }

    @Override
    public void update(double dt, GameState state) {
        wingPhase += dt;

        if (bounceTimer > 0) {
            x += bounceVx * dt;
            y += bounceVy * dt;
            bounceTimer -= dt;
            if (bounceTimer <= 0) {
                bounceVx = 0;
                bounceVy = 0;
                vx = 0;
                vy = 0;
            }
            updateFacing(bounceVx, bounceVy);
        } else {
            updateFacing(vx, vy);
            x += vx * dt;
            y += vy * dt;
        }

        x = Math.max(0, Math.min(x, GameConstants.LOGICAL_W - width));
        if (y + height < 0)                   y = GameConstants.LOGICAL_H;
        else if (y > GameConstants.LOGICAL_H)  y = -height;
    }

    private void updateFacing(double dvx, double dvy) {
        double speed = Math.sqrt(dvx * dvx + dvy * dvy);
        if (speed > 10.0) facingAngle = Math.atan2(dvy, dvx);
    }

    public void bounceBack(double cellCenterX, double cellCenterY) {
        double dy = getCenterY() - cellCenterY;
        double dist = Math.abs(dy);
        bounceVx = -BOUNCE_SPEED;
        bounceVy = (dist < 1.0) ? 0 : (dy / Math.max(dist, 1.0)) * BOUNCE_SPEED * 0.3;
        bounceTimer = BOUNCE_DURATION;
    }

    public boolean isBouncing() { return bounceTimer > 0; }

    public void setKeyboardVelocity(double vx, double vy) {
        if (bounceTimer > 0) return;
        this.vx = vx;
        this.vy = vy;
    }

    public boolean isInNeutralZone() {
        return getCenterX() >= GameConstants.NZ_LEFT && getCenterX() <= GameConstants.NZ_RIGHT;
    }

    public double[] getFacingDirection() {
        return new double[]{Math.cos(facingAngle), Math.sin(facingAngle)};
    }

    public double getFacingAngle() { return facingAngle; }
    public double getWingPhase()   { return wingPhase; }

    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.bounceTimer = 0;
        this.bounceVx = 0;
        this.bounceVy = 0;
        this.facingAngle = 0.0;
        this.wingPhase = 0.0;
        this.alive = true;
    }
}
