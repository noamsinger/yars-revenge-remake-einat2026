package com.yarsrevenge.model.wave;

public class WaveFactory {

    public static WaveConfig forWave(int wave) {
        double t = Math.min(1.0, (wave - 1) / 10.0);
        return new WaveConfig(
            lerp(70,  240, t),    // quotileSpeed px/sec
            lerp(0.75, 2.25, t),  // torpedoSpeed (scaled to px/sec in Torpedo)
            lerp(2.5, 0.7, t),    // bulletCooldown seconds
            (int) lerp(20, 10, t), // shieldRows
            wave % 2 == 0         // even waves: scrolling shield
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
