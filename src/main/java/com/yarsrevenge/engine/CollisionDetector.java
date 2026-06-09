package com.yarsrevenge.engine;

import com.yarsrevenge.config.GameConfig;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.*;

public class CollisionDetector {

    public static void detect(GameState state) {
        if (state.getPhase() != GameState.Phase.PLAYING) return;

        GameConfig.GameMode mode = GameConfig.getInstance().getGameMode();
        Player player = state.getPlayer();
        boolean playerInNZ = player.isInNeutralZone();

        // Player nibbles shield cell — bounce back
        // Normal/Rebound: charges cannon. Ultimate: accumulates Trons.
        if (!playerInNZ && !player.isBouncing()) {
            ShieldCell eaten = state.getShield().eatAt(
                player.getX(), player.getY(), player.getWidth(), player.getHeight());
            if (eaten != null) {
                state.addScore(61);
                if (mode == GameConfig.GameMode.ULTIMATE) {
                    state.addTron();
                } else {
                    state.chargeCanon();
                }
                player.bounceBack(eaten.x + eaten.cellWidth / 2.0, eaten.y + eaten.cellHeight / 2.0);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_NIBBLE);
            }
        }

        // Player touches Quotile body
        // Normal/Rebound: charges cannon. Ultimate: accumulates Trons.
        if (!playerInNZ && player.intersects(state.getQuotile())) {
            if (mode == GameConfig.GameMode.ULTIMATE) {
                state.addTron();
                state.addTron(); // touching Quotile = +2 Trons
            } else {
                state.chargeCanon();
            }
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

        // Player hit by Torpedo — Neutral Zone protects; debug mode skips; Torpedo is indestructible
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
                state.addScore(69);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_CELL_POP);
            }
        }

        // Swirl hits player — NZ offers no protection
        Swirl swirl = state.getSwirl();
        if (swirl != null && swirl.isAlive() && player.intersects(swirl)) {
            swirl.kill();
            state.setSwirl(null);
            state.getQuotile().swirlFinished();
            state.queueAudio(GameState.AudioEvent.STOP_SWIRL_LOOP);
            killPlayer(state);
            return;
        }

        // Zorlon Cannon interactions
        ZorlonCannon cannon = state.getZorlonCannon();
        if (cannon != null && cannon.isAlive()) {
            // Bouncing cannon hits player
            if (cannon.isBouncing() && cannon.intersects(player)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                if (mode == GameConfig.GameMode.ULTIMATE) {
                    // Catching the rebounding cannon = +4 Trons (high-precision reward)
                    state.addTron(); state.addTron(); state.addTron(); state.addTron();
                } else {
                    killPlayer(state);
                }
                return;
            }

            ShieldCell hit = state.getShield().destroyAt(
                cannon.getX(), cannon.getY(), cannon.getWidth(), cannon.getHeight());
            if (hit != null) {
                state.addScore(5);
                state.queueAudio(GameState.AudioEvent.PLAY_SHIELD_CELL_POP);
                if (mode == GameConfig.GameMode.REBOUND && !cannon.isBouncing()) {
                    // Rebound: bounce cannon back instead of killing it
                    cannon.bounce();
                } else {
                    cannon.kill();
                    state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                }
                return;
            }

            // Non-bouncing cannon hits player — self-kill
            if (!cannon.isBouncing() && cannon.intersects(player)) {
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

            // Cannon hits Swirl — bonus points, destroys Quotile
            Swirl swirl2 = state.getSwirl();
            if (swirl2 != null && swirl2.isAlive() && cannon.intersects(swirl2)) {
                cannon.kill();
                state.queueAudio(GameState.AudioEvent.STOP_CANNON_FLY);
                state.queueAudio(GameState.AudioEvent.STOP_SWIRL_LOOP);
                state.setExplosionPoint(swirl2.getCenterX(), swirl2.getCenterY());
                swirl2.kill();
                state.setSwirl(null);
                state.getQuotile().swirlFinished();
                state.addScore(6000); // sniper bonus for mid-air Swirl kill
                state.addLife();      // extra life, capped at 4
                destroyQuotile(state);
                return;
            }
        }
    }

    /** Check Swirl vs player — called every tick while a Swirl is in flight. */
    public static void detectSwirlOnly(GameState state) {
        if (state.getPhase() != GameState.Phase.PLAYING) return;
        Swirl swirl = state.getSwirl();
        if (swirl != null && swirl.isAlive() && state.getPlayer().intersects(swirl)) {
            swirl.kill();
            state.setSwirl(null);
            state.getQuotile().swirlFinished();
            state.queueAudio(GameState.AudioEvent.STOP_SWIRL_LOOP);
            killPlayer(state);
        }
    }

    private static void killPlayer(GameState state) {
        if (state.isDebugMode()) return;
        state.getPlayer().kill();
        state.loseLife();
        state.queueAudio(GameState.AudioEvent.PLAY_PLAYER_DEATH);
        state.setPhase(GameState.Phase.PLAYER_DYING);
    }

    private static void destroyQuotile(GameState state) {
        state.queueAudio(GameState.AudioEvent.STOP_ALL_LOOPS);
        state.queueAudio(GameState.AudioEvent.PLAY_QUOTILE_EXPLODE);
        state.addScore(1000);
        state.setPhase(GameState.Phase.WAVE_TRANSITION);
    }
}
