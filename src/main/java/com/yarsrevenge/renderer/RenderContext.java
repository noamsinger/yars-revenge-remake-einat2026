package com.yarsrevenge.renderer;

import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.*;
import java.util.List;

public record RenderContext(
    double playerX, double playerY, double playerW, double playerH,
    boolean playerInNZ,
    double playerFacingAngle, double playerWingPhase,
    double quotileX, double quotileY, double quotileW, double quotileH, boolean quotileAlive,
    com.yarsrevenge.model.entity.Quotile.Mode quotileMode, double quotileModeTimer,
    double torpedoX, double torpedoY, double torpedoW, double torpedoH, boolean torpedoAlive,
    List<QuotileShot> shots,
    List<PlayerBullet> playerBullets,
    Shield shield,
    ZorlonCannon zorlonCannon,
    QuotileMissile quotileMissile,
    int lives, int score, int wave,
    boolean cannonCharged,
    boolean debugMode,
    double explosionX, double explosionY,
    GameState.Phase phase,
    double phaseTimer,
    int pauseSelection
) {
    public static RenderContext snapshot(GameState s) {
        return new RenderContext(
            s.getPlayer().getX(), s.getPlayer().getY(),
            s.getPlayer().getWidth(), s.getPlayer().getHeight(),
            s.getPlayer().isInNeutralZone(),
            s.getPlayer().getFacingAngle(), s.getPlayer().getWingPhase(),
            s.getQuotile().getX(), s.getQuotile().getY(),
            s.getQuotile().getWidth(), s.getQuotile().getHeight(),
            s.getQuotile().isAlive(),
            s.getQuotile().getMode(), s.getQuotile().getModeTimer(),
            s.getTorpedo().getX(), s.getTorpedo().getY(),
            s.getTorpedo().getWidth(), s.getTorpedo().getHeight(),
            s.getTorpedo().isAlive(),
            s.getActiveShots(),
            s.getPlayerBullets(),
            s.getShield(),
            s.getZorlonCannon(),
            s.getQuotileMissile(),
            s.getLives(), s.getScore(), s.getCurrentWave(),
            s.isCannonCharged(),
            s.isDebugMode(),
            s.getExplosionX(), s.getExplosionY(),
            s.getPhase(), s.getPhaseTimer(),
            s.getPauseSelection()
        );
    }
}
