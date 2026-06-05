package com.yarsrevenge.audio;

import javafx.scene.media.AudioClip;
import java.io.IOException;
import java.nio.file.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioManager {

    private static AudioManager instance;

    private final Map<SoundEffect, AudioClip> clips = new EnumMap<>(SoundEffect.class);
    // Duration in ms for each looping clip — used by the scheduler
    private final Map<SoundEffect, Long> clipDurationMs = new EnumMap<>(SoundEffect.class);
    private boolean muted = false;

    // Scheduler for looping: fires clip.play(volume) at the clip's period
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "audio-loop-scheduler");
            t.setDaemon(true);
            return t;
        });

    // One-shot executor for non-looping plays
    private final ExecutorService audioThread =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "audio-worker");
            t.setDaemon(true);
            return t;
        });

    // Active loop handles — cancel to stop the loop
    private ScheduledFuture<?> bgHumFuture     = null;
    private ScheduledFuture<?> cannonFlyFuture = null;
    private ScheduledFuture<?> alarmFuture     = null;
    private ScheduledFuture<?> missileFuture   = null;

    private AudioManager() {
        // Clip durations must match PcmSynthesizer output lengths exactly
        clipDurationMs.put(SoundEffect.BG_HUM,         4000L);
        clipDurationMs.put(SoundEffect.CANNON_FLYING,  1500L);
        clipDurationMs.put(SoundEffect.ENEMY_ALARM,     240L);
        clipDurationMs.put(SoundEffect.QUOTILE_MISSILE, 1000L);

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("yars-audio-");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            System.err.println("Cannot create temp dir for audio: " + e.getMessage());
            return;
        }

        for (SoundEffect fx : SoundEffect.values()) {
            try {
                byte[] wav = PcmSynthesizer.generate(fx);
                Path wavFile = tempDir.resolve(fx.name().toLowerCase() + ".wav");
                Files.write(wavFile, wav);
                wavFile.toFile().deleteOnExit();
                AudioClip clip = new AudioClip(wavFile.toUri().toString());
                // Pre-warm: initialise the media engine now (on FX thread during startup)
                // so first play doesn't stall the FX thread mid-game.
                clip.play(0.0);
                clip.stop();
                clips.put(fx, clip);
            } catch (Exception e) {
                System.err.println("Failed to generate sound: " + fx + " — " + e.getMessage());
            }
        }
    }

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    // ---- fire-and-forget one-shot plays ----

    public void play(SoundEffect fx) {
        if (muted) return;
        AudioClip clip = clips.get(fx);
        if (clip == null) return;
        audioThread.submit(() -> clip.play(1.0));
    }

    public void play(SoundEffect fx, double volume) {
        if (muted) return;
        AudioClip clip = clips.get(fx);
        if (clip == null) return;
        audioThread.submit(() -> clip.play(volume));
    }

    // ---- loop management — uses ScheduledExecutorService, never setCycleCount ----

    public void startBgHum() {
        stopLoop(bgHumFuture);
        bgHumFuture = null;
        if (muted) return;
        bgHumFuture = startLoop(SoundEffect.BG_HUM, 0.75);
    }

    public void stopBgHum() {
        bgHumFuture = stopLoop(bgHumFuture);
    }

    public void startCannonFly() {
        stopLoop(cannonFlyFuture);
        cannonFlyFuture = null;
        if (muted) return;
        cannonFlyFuture = startLoop(SoundEffect.CANNON_FLYING, 0.6);
    }

    public void stopCannonFly() {
        cannonFlyFuture = stopLoop(cannonFlyFuture);
    }

    public void startAlarmLoop() {
        stopLoop(alarmFuture);
        alarmFuture = null;
        if (muted) return;
        alarmFuture = startLoop(SoundEffect.ENEMY_ALARM, 0.7);
    }

    public void stopAlarmLoop() {
        alarmFuture = stopLoop(alarmFuture);
    }

    public void startMissileLoop() {
        stopLoop(missileFuture);
        missileFuture = null;
        if (muted) return;
        missileFuture = startLoop(SoundEffect.QUOTILE_MISSILE, 0.75);
    }

    public void stopMissileLoop() {
        missileFuture = stopLoop(missileFuture);
    }

    public void pauseAllLoops() {
        bgHumFuture     = stopLoop(bgHumFuture);
        cannonFlyFuture = stopLoop(cannonFlyFuture);
        alarmFuture     = stopLoop(alarmFuture);
        missileFuture   = stopLoop(missileFuture);
    }

    public void resumeBgHum() { startBgHum(); }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) pauseAllLoops();
    }

    public boolean isMuted() { return muted; }

    // ---- internal helpers ----

    /** Start a clip looping by re-scheduling play() at the clip's exact duration. */
    private ScheduledFuture<?> startLoop(SoundEffect fx, double volume) {
        AudioClip clip = clips.get(fx);
        if (clip == null) return null;
        long periodMs = clipDurationMs.getOrDefault(fx, 500L);
        // Play immediately, then repeat every periodMs
        return scheduler.scheduleAtFixedRate(
            () -> clip.play(volume),
            0L, periodMs, TimeUnit.MILLISECONDS
        );
    }

    /** Cancel a loop future; returns null so callers can clear their reference. */
    private ScheduledFuture<?> stopLoop(ScheduledFuture<?> future) {
        if (future != null) future.cancel(false);
        return null;
    }
}
