package com.yarsrevenge.model;

public final class GameConstants {

    private GameConstants() {}

    public static final double LOGICAL_W = 1920.0;
    public static final double LOGICAL_H = 1080.0;

    public static final double NZ_LEFT  = 624.0;
    public static final double NZ_RIGHT = 1040.0;
    public static final double NZ_WIDTH = NZ_RIGHT - NZ_LEFT;

    public static final double SHIELD_X      = 1350.0;
    public static final double SHIELD_Y      =  100.0;
    public static final double SHIELD_HEIGHT =  880.0;
    public static final int    SHIELD_COLS   =  5;

    public static final double QUOTILE_X     = 1824.0;
    public static final double QUOTILE_W     =   64.0;
    public static final double QUOTILE_H     =   64.0;

    public static final double PLAYER_W      =   64.0;
    public static final double PLAYER_H      =   64.0;
    public static final double PLAYER_SPEED  =  700.0;

    public static final double BULLET_SPEED  =  500.0;
    public static final double CANNON_SPEED  =  900.0;

    public static final double SWIRL_ORBIT_RADIUS = 140.0;

    public static final double WAVE_TRANSITION_SECS = 5.0;
    public static final double PLAYER_DYING_SECS    = 1.5;
}
