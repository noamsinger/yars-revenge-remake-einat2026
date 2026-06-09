package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

/** The Swirl — Qotile transforms into this spinning pinwheel and launches it at the player. */
public class Swirl extends GameEntity {

    public static final double SIZE  = GameConstants.QUOTILE_W;
    public static final double SPEED = 1680.0;

    // Stall behavior (150k+ score): freeze mid-flight to bait the player
    private static final double STALL_AFTER   = 0.35; // seconds in flight before first stall
    private static final double STALL_DURATION = 0.55; // how long the freeze lasts
    private static final double STALL_INTERVAL = 1.20; // seconds between subsequent stalls

    private final double vx;
    private final double vy;
    private final double angle;
    private final boolean stalling;
    private final double launchTargetY; // player Y when swirl was fired

    private double flightTimer  = 0.0;
    private double stallTimer   = 0.0;
    private boolean stalled     = false;
    private double nextStallAt;

    public Swirl(double startX, double startY, double targetX, double targetY,
                 double speed, boolean stalling) {
        super(startX - SIZE / 2.0, startY - SIZE / 2.0, SIZE, SIZE);
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1.0) dist = 1.0;
        this.vx       = (dx / dist) * speed;
        this.vy       = (dy / dist) * speed;
        this.angle    = Math.atan2(dy, dx);
        this.stalling    = stalling;
        this.nextStallAt = STALL_AFTER;
        this.launchTargetY = targetY;
    }

    @Override
    public void update(double dt, GameState state) {
        flightTimer += dt;

        if (stalling) {
            if (stalled) {
                stallTimer += dt;
                if (stallTimer >= STALL_DURATION) {
                    stalled    = false;
                    stallTimer = 0.0;
                    nextStallAt = flightTimer + STALL_INTERVAL;
                }
                // Frozen — don't move
                return;
            } else if (flightTimer >= nextStallAt) {
                stalled = true;
                return;
            }
        }

        x += vx * dt;
        y += vy * dt;

        if (x + width  < -100 || x > GameConstants.LOGICAL_W + 100 ||
            y + height < -100 || y > GameConstants.LOGICAL_H + 100) {
            alive = false;
        }
    }

    public double getAngle()        { return angle; }
    public boolean isStalled()      { return stalled; }
    public double getLaunchTargetY(){ return launchTargetY; }
}
