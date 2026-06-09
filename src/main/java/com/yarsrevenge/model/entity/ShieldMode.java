package com.yarsrevenge.model.entity;

public enum ShieldMode {
    ARCH_BARRICADE,   // static arc around Quotile (original odd-wave)
    CYCLING_FENCE,    // flat rect wall; odd rows scroll L→R, even rows R→L; cells wrap to next row
    ROTATING_CIRCLE,  // arc orbits slowly around Quotile
    RANDOM_SWARM      // cells placed in random cloud, drift gently
}
