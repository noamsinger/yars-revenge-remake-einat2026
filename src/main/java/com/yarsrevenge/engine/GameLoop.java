package com.yarsrevenge.engine;

import com.yarsrevenge.audio.AudioManager;
import com.yarsrevenge.app.SceneManager;
import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.controller.GameController;
import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.Quotile;
import com.yarsrevenge.model.entity.ShieldMode;
import com.yarsrevenge.renderer.GameRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameLoop {

    private static final Logger log = LoggerFactory.getLogger(GameLoop.class);

    private static final double TICK_RATE = 1.0 / 60.0;

    private final GameState state;
    private final GameController controller;
    private final GameRenderer renderer;
    private final GraphicsContext gc;

    private AnimationTimer timer;
    private long lastNanos = -1;
    private double accumulator = 0.0;
    private double renderAccumulator = 0.0;

    public GameLoop(GameState state, GameController controller,
                    GameRenderer renderer, GraphicsContext gc) {
        this.state = state;
        this.controller = controller;
        this.renderer = renderer;
        this.gc = gc;
    }

    private Quotile.Mode lastQMode = Quotile.Mode.NORMAL;

    public void start() {
        lastNanos = -1;
        accumulator = 0.0;
        renderAccumulator = 0.0;
        lastQMode = Quotile.Mode.NORMAL;
        timer = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                long frameStart = System.nanoTime();
                if (lastNanos < 0) { lastNanos = nowNanos; return; }

                double delta = (nowNanos - lastNanos) / 1_000_000_000.0;
                lastNanos = nowNanos;
                // Log any frame gap > 200ms — indicates FX thread was blocked between calls
                if (delta > 0.20) {
                    log.warn("Frame gap {}ms phase={} qMode={}", (long)(delta * 1000), state.getPhase(), state.getQuotile().getMode());
                }
                if (delta > 0.25) delta = 0.25;

                accumulator += delta;
                while (accumulator >= TICK_RATE) {
                    controller.processInput(TICK_RATE, state);
                    tick(TICK_RATE);
                    accumulator -= TICK_RATE;
                }

                // Drain audio events outside the tick loop — safe to call AudioClip here
                java.util.List<GameState.AudioEvent> events = state.drainAudioEvents();
                if (!events.isEmpty()) {
                    long drainStart = System.nanoTime();
                    for (GameState.AudioEvent ev : events) {
                        long evStart = System.nanoTime();
                        switch (ev) {
                            case STOP_ALL_LOOPS    -> AudioManager.getInstance().pauseAllLoops();
                            case START_ALARM       -> AudioManager.getInstance().startAlarmLoop();
                            case STOP_ALARM        -> AudioManager.getInstance().stopAlarmLoop();
                            case START_SWIRL_LOOP  -> AudioManager.getInstance().startSwirlLoop();
                            case STOP_SWIRL_LOOP   -> AudioManager.getInstance().stopSwirlLoop();
                            case RESUME_BG_HUM     -> AudioManager.getInstance().resumeBgHum();
                            case STOP_CANNON_FLY   -> AudioManager.getInstance().stopCannonFly();
                            case START_CANNON_FLY  -> AudioManager.getInstance().startCannonFly();
                            case PLAY_SHIELD_NIBBLE   -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.SHIELD_NIBBLE);
                            case PLAY_SHIELD_CELL_POP -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.SHIELD_CELL_POP);
                            case PLAY_PLAYER_DEATH    -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.PLAYER_DEATH);
                            case PLAY_QUOTILE_EXPLODE -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.QUOTILE_EXPLODE);
                            case PLAY_BULLET_SHOOT    -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.BULLET_SHOOT);
                            case PLAY_CANNON_LAUNCH   -> AudioManager.getInstance().play(com.yarsrevenge.audio.SoundEffect.CANNON_LAUNCH);
                        }
                        long evMs = (System.nanoTime() - evStart) / 1_000_000;
                        if (evMs > 5) log.warn("Audio drain: {} took {}ms", ev, evMs);
                    }
                    long totalMs = (System.nanoTime() - drainStart) / 1_000_000;
                    if (totalMs > 5) log.warn("Audio drain total {}ms for {}", totalMs, events);
                }

                // Throttle rendering to configured FPS (simulation always runs at 60 Hz)
                double renderInterval = 1.0 / GameConfig.getInstance().getFpsLimit();
                renderAccumulator += delta;
                boolean doRender = false;
                while (renderAccumulator >= renderInterval) {
                    renderAccumulator -= renderInterval;
                    doRender = true;
                }
                if (doRender) {
                    long renderStart = System.nanoTime();
                    renderer.render(gc, state);
                    long renderMs = (System.nanoTime() - renderStart) / 1_000_000;
                    if (renderMs > 20) log.warn("Render took {}ms", renderMs);
                }

                long frameMs = (System.nanoTime() - frameStart) / 1_000_000;
                if (frameMs > 50) log.warn("Slow frame {}ms phase={}", frameMs, state.getPhase());
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; }
    }

    private void tick(double dt) {
        switch (state.getPhase()) {
            case PLAYING -> {
                Quotile.Mode qModeBefore = state.getQuotile().getMode();

                long t0 = System.nanoTime();
                state.getQuotile().update(dt, state);
                long t1 = System.nanoTime();
                double qcy = state.getQuotile().getCenterY();
                switch (state.getWaveConfig().shieldMode()) {
                    case CYCLING_FENCE  -> state.getShield().updateScrolling(qcy, dt);
                    case ROTATING_CIRCLE -> state.getShield().updateRotating(qcy, dt);
                    case RANDOM_SWARM    -> state.getShield().updateSwarm(qcy, dt);
                    default              -> state.getShield().update(qcy); // ARCH_BARRICADE
                }
                long t2 = System.nanoTime();

                Quotile.Mode qModeAfter = state.getQuotile().getMode();

                // Log when entering missile alarm mode
                if (qModeAfter != qModeBefore) {
                    log.debug("Quotile mode {} -> {} quotileUpdate={}ms shieldUpdate={}ms",
                        qModeBefore, qModeAfter, (t1-t0)/1_000_000, (t2-t1)/1_000_000);
                }

                boolean inMissileMode = qModeAfter == Quotile.Mode.MISSILE_WARNING
                                  || qModeAfter == Quotile.Mode.MISSILE_FIRED;

                boolean wasInMissileMode = lastQMode == Quotile.Mode.MISSILE_WARNING
                                     || lastQMode == Quotile.Mode.MISSILE_FIRED;

                // Missile alarm just exited
                if (!inMissileMode && wasInMissileMode) {
                    log.debug("Missile mode exited: {} -> {} => RESUME_BG_HUM", lastQMode, qModeAfter);
                    state.queueAudio(GameState.AudioEvent.RESUME_BG_HUM);
                }
                lastQMode = qModeAfter;

                // Player, bullets and cannon always update — player must be able to dodge the missile
                state.getPlayer().update(dt, state);
                state.getActiveShots().forEach(b -> b.update(dt, state));
                state.getActiveShots().removeIf(b -> !b.isAlive());
                state.getPlayerBullets().forEach(b -> b.update(dt, state));
                state.getPlayerBullets().removeIf(b -> !b.isAlive());
                if (state.getZorlonCannon() != null) {
                    state.getZorlonCannon().update(dt, state);
                    if (!state.getZorlonCannon().isAlive()) state.setZorlonCannon(null);
                }

                // Torpedo always moves — it is indestructible
                state.getTorpedo().update(dt, state);

                // Full collision detection always runs (cannon must be able to kill quotile in missile mode)
                CollisionDetector.detect(state);

                // Swirl always moves during MISSILE_FIRED
                if (state.getSwirl() != null) {
                    state.getSwirl().update(dt, state);
                    if (!state.getSwirl().isAlive()) {
                        state.setSwirl(null);
                        state.getQuotile().swirlFinished();
                        state.queueAudio(GameState.AudioEvent.STOP_SWIRL_LOOP);
                    }
                    CollisionDetector.detectSwirlOnly(state);
                }
            }
            case PAUSED -> { /* waiting for user choice — renderer draws overlay */ }
            case PLAYER_DYING -> {
                state.advancePhaseTimer(dt);
                if (state.getPhaseTimer() >= GameConstants.PLAYER_DYING_SECS) {
                    if (state.getLives() <= 0) {
                        state.setPhase(GameState.Phase.GAME_OVER);
                        AudioManager.getInstance().pauseAllLoops();
                        stop();
                        SceneManager.getInstance().showGameOver(state.getScore(), state.getCurrentWave());
                    } else {
                        state.respawnPlayer();
                        state.setPhase(GameState.Phase.PLAYING);
                        state.queueAudio(GameState.AudioEvent.RESUME_BG_HUM);
                    }
                }
            }
            case WAVE_TRANSITION -> {
                state.advancePhaseTimer(dt);
                if (state.getPhaseTimer() >= GameConstants.WAVE_TRANSITION_SECS) {
                    state.initWave(state.getCurrentWave() + 1);
                    lastQMode = Quotile.Mode.NORMAL; // reset so missile-exit check doesn't misfire
                    state.setPhase(GameState.Phase.PLAYING);
                    state.queueAudio(GameState.AudioEvent.RESUME_BG_HUM);
                }
            }
            case GAME_OVER -> { /* handled above */ }
        }
    }
}
