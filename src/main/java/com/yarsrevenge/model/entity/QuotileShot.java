package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

/** Projectile fired by the Quotile at the player. */
public class QuotileShot extends GameEntity {

    public static final double SHOT_W = 16.0;
    public static final double SHOT_H =  8.0;

    public QuotileShot(double x, double y) {
        super(x, y, SHOT_W, SHOT_H);
    }

    @Override
    public void update(double dt, GameState state) {
        x -= GameConstants.SHOT_SPEED * dt;
        if (x + width < 0 || x > GameConstants.NZ_RIGHT) {
            kill();
        }
    }
}
