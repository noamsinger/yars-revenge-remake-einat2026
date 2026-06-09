package com.yarsrevenge.model.wave;

import com.yarsrevenge.model.entity.ShieldMode;

public record WaveConfig(
    double quotileSpeed,
    double torpedoSpeed,
    double bulletCooldown,
    int shieldRows,
    ShieldMode shieldMode,
    double missileSpeedMultiplier
) {
    public boolean scrollingShield() {
        return shieldMode == ShieldMode.CYCLING_FENCE;
    }
}
