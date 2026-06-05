package com.yarsrevenge.model.wave;

public record WaveConfig(
    double quotileSpeed,
    double swirlAngularSpeed,
    double bulletCooldown,
    int shieldRows
) {}
