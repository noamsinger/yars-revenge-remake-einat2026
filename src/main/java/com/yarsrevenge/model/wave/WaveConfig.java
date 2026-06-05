package com.yarsrevenge.model.wave;

public record WaveConfig(
    double quotileSpeed,
    double torpedoSpeed,
    double bulletCooldown,
    int shieldRows,
    boolean scrollingShield
) {}
