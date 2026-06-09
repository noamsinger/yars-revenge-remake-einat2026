package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class ZorlonCannon extends GameEntity {

    private static final double BEAM_W = 60.0;
    private static final double BEAM_H = 12.0;

    private int direction = 1; // +1 = rightward, -1 = leftward (Rebound mode)

    public ZorlonCannon(double startY) {
        super(0, startY - BEAM_H / 2.0, BEAM_W, BEAM_H);
    }

    /** Reverse direction — used in Rebound mode when hitting the shield. */
    public void bounce() { direction = -1; }

    public boolean isBouncing() { return direction < 0; }

    @Override
    public void update(double dt, GameState state) {
        x += direction * GameConstants.CANNON_SPEED * dt;
        if (direction > 0 && x > GameConstants.LOGICAL_W) {
            state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
            kill();
        } else if (direction < 0 && x + width < 0) {
            state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
            kill();
        }
    }
}
