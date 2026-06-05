package com.yarsrevenge.model;

import com.yarsrevenge.model.entity.*;
import com.yarsrevenge.model.wave.WaveConfig;
import com.yarsrevenge.model.wave.WaveFactory;
import java.util.ArrayList;
import java.util.List;

public class GameState {

    public enum Phase { PLAYING, PAUSED, PLAYER_DYING, WAVE_TRANSITION, GAME_OVER }

    /** Audio events entities can request without calling AudioManager directly. */
    public enum AudioEvent {
        STOP_ALL_LOOPS, START_ALARM, STOP_ALARM,
        START_MISSILE_LOOP, STOP_MISSILE_LOOP, RESUME_BG_HUM,
        STOP_CANNON_FLY, START_CANNON_FLY,
        PLAY_SHIELD_NIBBLE, PLAY_SHIELD_CELL_POP,
        PLAY_PLAYER_DEATH, PLAY_QUOTILE_EXPLODE,
        PLAY_BULLET_SHOOT, PLAY_CANNON_LAUNCH
    }

    private Player player;
    private Quotile quotile;
    private Torpedo torpedo;
    private Shield shield;
    private ZorlonCannon zorlonCannon;
    private QuotileMissile quotileMissile;
    private final List<QuotileShot> activeShots    = new ArrayList<>();
    private final List<PlayerBullet> playerBullets = new ArrayList<>();
    private final List<AudioEvent> pendingAudio    = new ArrayList<>();

    private int lives = 4;
    private int score = 0;
    private int currentWave = 1;
    private boolean cannonCharged = false;
    private boolean debugMode = false;
    private double explosionX = GameConstants.QUOTILE_X + GameConstants.QUOTILE_W / 2.0;
    private double explosionY = GameConstants.LOGICAL_H / 2.0;
    private WaveConfig waveConfig;

    private double torpedoRespawnTimer = 0.0;
    private static final double TORPEDO_RESPAWN_DELAY = 4.0;

    private Phase phase = Phase.PLAYING;
    private double phaseTimer = 0.0;
    private int pauseSelection = 0;

    public int getPauseSelection()       { return pauseSelection; }
    public void setPauseSelection(int s) { pauseSelection = s; }

    public GameState() {
        initWave(1);
    }

    public void initWave(int wave) {
        currentWave = wave;
        waveConfig = WaveFactory.forWave(wave);
        activeShots.clear();
        playerBullets.clear();
        zorlonCannon = null;
        quotileMissile = null;
        cannonCharged = false;

        shield = new Shield(GameConstants.SHIELD_COLS, waveConfig.shieldRows());
        if (waveConfig.scrollingShield()) {
            shield.rebuildRect(waveConfig.shieldRows());
        } else {
            shield.rebuild(waveConfig.shieldRows());
        }

        if (quotile == null) {
            quotile = new Quotile(waveConfig.quotileSpeed(), waveConfig.bulletCooldown());
        } else {
            quotile.setSpeed(waveConfig.quotileSpeed());
            quotile.setBulletCooldown(waveConfig.bulletCooldown());
            quotile.reset();
        }

        if (torpedo == null) {
            torpedo = new Torpedo(quotile, waveConfig.torpedoSpeed());
        } else {
            torpedo.setSpeed(waveConfig.torpedoSpeed());
            torpedo.respawn();
        }

        if (player == null) {
            player = new Player(200, GameConstants.LOGICAL_H / 2.0 - GameConstants.PLAYER_H / 2.0);
        } else {
            player.reset(200, GameConstants.LOGICAL_H / 2.0 - GameConstants.PLAYER_H / 2.0);
        }
    }

    public void respawnPlayer() {
        player.reset(200, GameConstants.LOGICAL_H / 2.0 - GameConstants.PLAYER_H / 2.0);
        activeShots.clear();
        playerBullets.clear();
        torpedo.respawn();
    }

    public void addShot(QuotileShot s)           { activeShots.add(s); }
    public void addPlayerBullet(PlayerBullet b)  { playerBullets.add(b); }
    public void addScore(int pts)                { score += pts; }
    public void loseLife()          { lives = Math.max(0, lives - 1); }
    public void chargeCanon()       { cannonCharged = true; }
    public void dischargeCanon()    { cannonCharged = false; }

    public void setPhase(Phase p) {
        phase = p;
        phaseTimer = 0.0;
        if (p == Phase.PAUSED) pauseSelection = 0;
    }
    public void advancePhaseTimer(double dt) { phaseTimer += dt; }

    public void advanceTorpedoRespawnTimer(double dt) {
        torpedoRespawnTimer += dt;
        if (torpedoRespawnTimer >= TORPEDO_RESPAWN_DELAY) {
            torpedoRespawnTimer = 0.0;
            torpedo.respawn();
        }
    }

    public Player getPlayer()                    { return player; }
    public Quotile getQuotile()                  { return quotile; }
    public Torpedo getTorpedo()                  { return torpedo; }
    public Shield getShield()                    { return shield; }
    public ZorlonCannon getZorlonCannon()        { return zorlonCannon; }
    public QuotileMissile getQuotileMissile()    { return quotileMissile; }
    public List<QuotileShot> getActiveShots()    { return activeShots; }
    public List<PlayerBullet> getPlayerBullets() { return playerBullets; }
    public int getLives()                        { return lives; }
    public int getScore()                        { return score; }
    public int getCurrentWave()                  { return currentWave; }
    public WaveConfig getWaveConfig()            { return waveConfig; }
    public Phase getPhase()                      { return phase; }
    public double getPhaseTimer()                { return phaseTimer; }
    public boolean isCannonCharged()             { return cannonCharged; }
    public boolean isDebugMode()                 { return debugMode; }
    public void toggleDebugMode()                { debugMode = !debugMode; }
    public double getExplosionX()                { return explosionX; }
    public double getExplosionY()                { return explosionY; }
    public void setExplosionPoint(double x, double y) { explosionX = x; explosionY = y; }

    public void setZorlonCannon(ZorlonCannon c)     { zorlonCannon = c; }
    public void setQuotileMissile(QuotileMissile m) { quotileMissile = m; }

    public void queueAudio(AudioEvent e) { pendingAudio.add(e); }
    public List<AudioEvent> drainAudioEvents() {
        List<AudioEvent> copy = new ArrayList<>(pendingAudio);
        pendingAudio.clear();
        return copy;
    }
}
