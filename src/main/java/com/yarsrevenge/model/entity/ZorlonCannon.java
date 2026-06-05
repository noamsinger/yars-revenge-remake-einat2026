package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class ZorlonCannon extends GameEntity {

    private static final double BEAM_W = 60.0;
    private static final double BEAM_H = 12.0;

    public ZorlonCannon(double startY) {
        super(0, startY - BEAM_H / 2.0, BEAM_W, BEAM_H);
    }

    @Override
    public void update(double dt, GameState state) {
        x += GameConstants.CANNON_SPEED * dt;
        if (x > GameConstants.LOGICAL_W) {
            state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
            kill();
        }
    }
}
