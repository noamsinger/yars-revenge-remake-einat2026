package com.yarsrevenge.model.entity;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;

public class Quotile extends GameEntity {

    public enum Mode { NORMAL, MISSILE_WARNING, MISSILE_FIRED }

    public static final double MISSILE_WARNING_AFTER      = 15.0;
    public static final double MISSILE_WARNING_AFTER_HIGH = 0.1;  // 70k+: effectively instant
    public static final double MISSILE_WARNING_SECS       = 4.0;
    public static final double MISSILE_WARNING_SECS_HIGH  = 0.05; // 70k+: no visible alarm phase

    private double speed;
    private int direction = 1;
    private double bulletCooldown;
    private double bulletTimer = 0;

    private Mode mode = Mode.NORMAL;
    private double modeTimer = 0.0;

    public Quotile(double speed, double bulletCooldown) {
        super(GameConstants.QUOTILE_X,
              GameConstants.LOGICAL_H / 2.0 - GameConstants.QUOTILE_H / 2.0,
              GameConstants.QUOTILE_W,
              GameConstants.QUOTILE_H);
        this.speed = speed;
        this.bulletCooldown = bulletCooldown;
    }

    @Override
    public void update(double dt, GameState state) {
        modeTimer += dt;

        // Quotile always bounces up and down regardless of mode
        y += speed * direction * dt;
        Shield shield = state.getShield();
        double cellH  = ShieldCell.CELL_W;
        double minCY, maxCY;
        if (shield.isRectMode()) {
            // Clamp so the rect wall stays fully on screen
            double halfH = shield.getRectHalfHeight();
            minCY = halfH + cellH / 2.0;
            maxCY = GameConstants.LOGICAL_H - halfH - cellH / 2.0;
        } else {
            double minOff = shield.getMinCellOffsetY();
            double maxOff = shield.getMaxCellOffsetY();
            minCY = -minOff + cellH / 2.0;
            maxCY = GameConstants.LOGICAL_H - maxOff - cellH / 2.0;
        }
        if (getCenterY() <= minCY) { y = minCY - height / 2.0; direction =  1; }
        else if (getCenterY() >= maxCY) { y = maxCY - height / 2.0; direction = -1; }

        switch (mode) {
            case NORMAL -> {
                bulletTimer += dt;
                if (bulletTimer >= bulletCooldown) {
                    bulletTimer = 0;
                    fireBullet(state);
                }
                double warningAfter = state.getScore() >= 70_000
                    ? MISSILE_WARNING_AFTER_HIGH : MISSILE_WARNING_AFTER;
                if (modeTimer >= warningAfter) {
                    mode = Mode.MISSILE_WARNING;
                    modeTimer = 0.0;
                    state.queueAudio(GameState.AudioEvent.STOP_ALL_LOOPS);
                    state.queueAudio(GameState.AudioEvent.START_ALARM);
                }
            }
            case MISSILE_WARNING -> {
                double warningSecs = state.getScore() >= 70_000
                    ? MISSILE_WARNING_SECS_HIGH : MISSILE_WARNING_SECS;
                if (modeTimer >= warningSecs) {
                    mode = Mode.MISSILE_FIRED;
                    modeTimer = 0.0;
                    Player p = state.getPlayer();
                    double missileSpeed = Swirl.SPEED
                        * state.getWaveConfig().missileSpeedMultiplier();
                    boolean stalling = state.getScore() >= 150_000;
                    state.setSwirl(new Swirl(
                        getCenterX(), getCenterY(),
                        p.getCenterX(), p.getCenterY(), missileSpeed, stalling));
                    state.queueAudio(GameState.AudioEvent.STOP_ALARM);
                    state.queueAudio(GameState.AudioEvent.START_SWIRL_LOOP);
                }
            }
            case MISSILE_FIRED -> { /* waiting for missile to finish */ }
        }
    }

    private void fireBullet(GameState state) {
        double bx = x - QuotileShot.SHOT_W;
        double by = getCenterY() - QuotileShot.SHOT_H / 2.0;
        state.addShot(new QuotileShot(bx, by));
    }

    public void swirlFinished() {
        mode = Mode.NORMAL;
        modeTimer = 0.0;
        bulletTimer = 0.0;
    }

    public boolean isExposed(Shield shield) { return shield.isColumnClearAt(x); }
    public Mode getMode()                   { return mode; }
    public double getModeTimer()            { return modeTimer; }
    public double getSpeed()                { return speed; }
    public int getDirection()               { return direction; }
    public void setSpeed(double speed)      { this.speed = speed; }
    public void setBulletCooldown(double cd){ this.bulletCooldown = cd; }

    public void reset() {
        this.y          = GameConstants.LOGICAL_H / 2.0 - height / 2.0;
        this.direction  = 1;
        this.bulletTimer = 0;
        this.mode       = Mode.NORMAL;
        this.modeTimer  = 0.0;
        this.alive      = true;
    }
}
