package com.yarsrevenge.model.wave;

import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.model.entity.ShieldMode;

public class WaveFactory {

    private static final ShieldMode[] CYCLE = {
        ShieldMode.ARCH_BARRICADE,
        ShieldMode.CYCLING_FENCE,
        ShieldMode.ROTATING_CIRCLE,
        ShieldMode.RANDOM_SWARM
    };

    public static WaveConfig forWave(int wave) {
        GameConfig.GameMode mode = GameConfig.getInstance().getGameMode();
        double t = Math.min(1.0, (wave - 1) / 10.0);

        ShieldMode shieldMode = CYCLE[(wave - 1) % CYCLE.length];
        boolean fenceMult = shieldMode == ShieldMode.CYCLING_FENCE;
        double evenMult = fenceMult ? Math.pow(1.1, wave / 2) : 1.0;

        double torpedoSpeed = lerp(0.75, 2.25, t) * evenMult;
        if (mode == GameConfig.GameMode.NOVICE) torpedoSpeed *= 0.5;

        return new WaveConfig(
            lerp(70,  240, t),
            torpedoSpeed,
            lerp(2.5, 0.7, t),
            (int) lerp(20, 10, t),
            shieldMode,
            evenMult
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
