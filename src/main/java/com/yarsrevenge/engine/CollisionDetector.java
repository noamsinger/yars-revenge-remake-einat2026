package com.yarsrevenge.engine;

import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.*;

public class CollisionDetector {

    public static void detect(GameState state) {
        if (state.getPhase() != GameState.Phase.PLAYING) return;

        Player player = state.getPlayer();
        boolean playerInNZ = player.isInNeutralZone();

        // Player nibbles shield cell — bounce back and charge cannon
        if (!playerInNZ && !player.isBouncing()) {
            ShieldCell eaten = state.getShield().eatAt(
                player.getX(), player.getY(), player.getWidth(), player.getHeight());
            if (eaten != null) {
                state.addScore(10);
                state.chargeCanon();
                player.bounceBack(eaten.x + eaten.cellWidth / 2.0, eaten.y + eaten.cellHeight / 2.0);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_NIBBLE);
            }
        }

        // Player touches Quotile body — charges cannon
        if (!playerInNZ && player.intersects(state.getQuotile())) {
            state.chargeCanon();
        }

        // Player hit by Quotile shot — Neutral Zone blocks shots
        if (!playerInNZ) {
            for (QuotileShot s : state.getActiveShots()) {
                if (s.isAlive() && player.intersects(s)) {
                    s.kill();
                    killPlayer(state);
                    return;
                }
            }
        }

        // Player hit by Torpedo (Orb) — Neutral Zone protects; debug mode skips
        if (!playerInNZ && !state.isDebugMode() && state.getTorpedo().isAlive() && player.intersects(state.getTorpedo())) {
            killPlayer(state);
            return;
        }

        // Player bullets hitting shield only — cannot kill Torpedo or Quotile
        for (PlayerBullet pb : state.getPlayerBullets()) {
            if (!pb.isAlive()) continue;
            ShieldCell pbHit = state.getShield().destroyAt(
                pb.getX(), pb.getY(), pb.getWidth(), pb.getHeight());
            if (pbHit != null) {
                pb.kill();
                state.addScore(5);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_CELL_POP);
            }
        }

        // Quotile missile hits player — NZ offers no protection
        QuotileMissile missile = state.getQuotileMissile();
        if (missile != null && missile.isAlive() && player.intersects(missile)) {
            missile.kill();
            state.setQuotileMissile(null);
            state.getQuotile().missileFinished();
            state.getTorpedo().reset();
            state.queueAudio(GameState.AudioEvent.STOP_MISSILE_LOOP);
            killPlayer(state);
            return;
        }

        // Zorlon Cannon interactions
        ZorlonCannon cannon = state.getZorlonCannon();
        if (cannon != null && cannon.isAlive()) {
            ShieldCell hit = state.getShield().destroyAt(
                cannon.getX(), cannon.getY(), cannon.getWidth(), cannon.getHeight());
            if (hit != null) {
                state.addScore(5);
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_CELL_POP);
                return;
            }
            if (cannon.intersects(player)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                killPlayer(state);
                return;
            }
            if (cannon.intersects(state.getQuotile())) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.setExplosionPoint(state.getQuotile().getCenterX(), state.getQuotile().getCenterY());
                destroyQuotile(state);
                return;
            }
            QuotileMissile missile2 = state.getQuotileMissile();
            if (missile2 != null && missile2.isAlive() && cannon.intersects(missile2)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.queueAudio(GameState.AudioEvent.STOP_MISSILE_LOOP);
                state.setExplosionPoint(missile2.getCenterX(), missile2.getCenterY());
                missile2.kill();
                state.setQuotileMissile(null);
                state.getQuotile().missileFinished();
                state.getTorpedo().reset();
                destroyQuotile(state);
                return;
            }
        }
    }

    /** Check missile vs player — called every tick while a missile is in flight. */
    public static void detectMissileOnly(GameState state) {
        if (state.getPhase() != GameState.Phase.PLAYING) return;
        QuotileMissile missile = state.getQuotileMissile();
        if (missile != null && missile.isAlive() && state.getPlayer().intersects(missile)) {
            missile.kill();
            state.setQuotileMissile(null);
            state.getQuotile().missileFinished();
            state.getTorpedo().reset();
            state.queueAudio(GameState.AudioEvent.STOP_MISSILE_LOOP);
            killPlayer(state);
        }
    }

    private static void killPlayer(GameState state) {
        state.getPlayer().kill();
        state.loseLife();
        state.getTorpedo().reset();
        state.queueAudio(GameState.AudioEvent.PLAY_PLAYER_DEATH);
        state.setPhase(GameState.Phase.PLAYER_DYING);
    }

    private static void destroyQuotile(GameState state) {
        state.queueAudio(GameState.AudioEvent.STOP_ALL_LOOPS);
        state.queueAudio(GameState.AudioEvent.PLAY_QUOTILE_EXPLODE);
        state.addScore(1000 + state.getCurrentWave() * 200);
        state.setPhase(GameState.Phase.WAVE_TRANSITION);
    }
}
