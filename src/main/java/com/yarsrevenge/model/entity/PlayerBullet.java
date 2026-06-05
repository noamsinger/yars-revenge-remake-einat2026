package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class PlayerBullet extends GameEntity {

    public static final double W = 14.0;
    public static final double H =  6.0;
    private static final double SPEED = 700.0;

    private final double dx;
    private final double dy;

    public PlayerBullet(double x, double y, double dx, double dy) {
        super(x, y, W, H);
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void update(double dt, GameState state) {
        x += dx * SPEED * dt;
        y += dy * SPEED * dt;
        if (x > GameConstants.LOGICAL_W || x < -W
                || y > GameConstants.LOGICAL_H || y < -H) {
            kill();
        }
    }
}
