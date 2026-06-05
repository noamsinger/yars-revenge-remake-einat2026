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

        // Player hit by enemy bullet — Neutral Zone blocks bullets
        if (!playerInNZ) {
            for (Bullet b : state.getActiveBullets()) {
                if (b.isAlive() && player.intersects(b)) {
                    b.kill();
                    killPlayer(state);
                    return;
                }
            }
        }

        // Player hit by Swirl — Neutral Zone protects; debug mode skips
        if (!playerInNZ && !state.isDebugMode() && state.getSwirl().isAlive() && player.intersects(state.getSwirl())) {
            killPlayer(state);
            return;
        }

        // Player bullets hitting shield only — bullets cannot kill swirl or quotile
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
            state.getSwirl().reset();
            killPlayer(state);
            return;
        }

        // Zorlon Cannon interactions
        ZorlonCannon cannon = state.getZorlonCannon();
        if (cannon != null && cannon.isAlive()) {
            // Cannon destroys one shield cell and stops
            ShieldCell hit = state.getShield().destroyAt(
                cannon.getX(), cannon.getY(), cannon.getWidth(), cannon.getHeight());
            if (hit != null) {
                state.addScore(5);
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_CELL_POP);
                return;
            }
            // Cannon kills player if it hits them
            if (cannon.intersects(player)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                killPlayer(state);
                return;
            }
            // Cannon always kills Quotile on contact, regardless of shield state
            if (cannon.intersects(state.getQuotile())) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.setExplosionPoint(state.getQuotile().getCenterX(), state.getQuotile().getCenterY());
                destroyQuotile(state);
                return;
            }
            // Cannon also hits the flying missile (JetOctopus in MISSILE_FIRED mode)
            QuotileMissile missile2 = state.getQuotileMissile();
            if (missile2 != null && missile2.isAlive() && cannon.intersects(missile2)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.setExplosionPoint(missile2.getCenterX(), missile2.getCenterY());
                missile2.kill();
                state.setQuotileMissile(null);
                state.getQuotile().missileFinished();
                state.getSwirl().reset();
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
            state.getSwirl().reset();
            state.queueAudio(GameState.AudioEvent.STOP_MISSILE_LOOP);
            killPlayer(state);
        }
    }

    private static void killPlayer(GameState state) {
        state.getPlayer().kill();
        state.loseLife();
        state.getSwirl().reset();
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
