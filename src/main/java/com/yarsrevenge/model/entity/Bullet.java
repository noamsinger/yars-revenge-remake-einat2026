package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class Bullet extends GameEntity {

    public static final double BULLET_W = 16.0;
    public static final double BULLET_H =  8.0;

    public Bullet(double x, double y) {
        super(x, y, BULLET_W, BULLET_H);
    }

    @Override
    public void update(double dt, GameState state) {
        x -= GameConstants.BULLET_SPEED * dt;
        if (x + width < 0 || x > GameConstants.NZ_RIGHT) {
            kill();
        }
    }
}
