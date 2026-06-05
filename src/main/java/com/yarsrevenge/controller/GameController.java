package com.yarsrevenge.controller;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.PlayerBullet;
import com.yarsrevenge.model.entity.ZorlonCannon;
import javafx.scene.input.KeyCode;

public class GameController {

    private final InputHandler input;
    // Cooldown so holding SPACE doesn't spam bullets
    private double bulletCooldown = 0.0;
    private static final double BULLET_COOLDOWN_SECS = 0.25;

    public GameController(InputHandler input) {
        this.input = input;
    }

    public void processInput(double dt, GameState state) {
        if (state.getPhase() == GameState.Phase.PAUSED) {
            // ESC always resumes
            if (input.consumeEscape()) {
                state.setPhase(GameState.Phase.PLAYING);
                state.queueAudio(GameState.AudioEvent.RESUME_BG_HUM);
                return;
            }
            // UP/DOWN navigate between Resume (0) and Quit (1)
            if (input.isKeyDown(KeyCode.UP) || input.isKeyDown(KeyCode.W)) {
                state.setPauseSelection(0);
            } else if (input.isKeyDown(KeyCode.DOWN) || input.isKeyDown(KeyCode.S)) {
                state.setPauseSelection(1);
            }

            // ENTER or SPACE confirms selection
            boolean confirm = input.consumeCannonFire() || input.consumeBulletFire();
            if (confirm) {
                if (state.getPauseSelection() == 0) {
                    state.setPhase(GameState.Phase.PLAYING);
                    state.queueAudio(GameState.AudioEvent.RESUME_BG_HUM);
                } else {
                    state.setPhase(GameState.Phase.GAME_OVER);
                    com.yarsrevenge.app.SceneManager.getInstance().showMainMenu();
                }
            }
            return;
        }

        if (input.consumeEscape()) {
            state.setPhase(GameState.Phase.PAUSED);
            state.queueAudio(GameState.AudioEvent.STOP_ALL_LOOPS);
            return;
        }
        if (input.consumeDebugToggle()) {
            state.toggleDebugMode();
        }
        if (state.getPhase() != GameState.Phase.PLAYING) return;

        handleMovement(state);
        handleBulletFire(dt, state);
        handleCannonFire(state);
    }

    public void clearKeys() { input.clearKeys(); }

    private void handleMovement(GameState state) {
        double speed = GameConstants.PLAYER_SPEED;
        double vx = 0, vy = 0;
        boolean keyUsed = false;

        if (input.isKeyDown(KeyCode.LEFT)  || input.isKeyDown(KeyCode.A)) { vx -= speed; keyUsed = true; }
        if (input.isKeyDown(KeyCode.RIGHT) || input.isKeyDown(KeyCode.D)) { vx += speed; keyUsed = true; }
        if (input.isKeyDown(KeyCode.UP)    || input.isKeyDown(KeyCode.W)) { vy -= speed; keyUsed = true; }
        if (input.isKeyDown(KeyCode.DOWN)  || input.isKeyDown(KeyCode.S)) { vy += speed; keyUsed = true; }

        if (keyUsed) {
            if (vx != 0 && vy != 0) {
                double factor = 1.0 / Math.sqrt(2.0);
                vx *= factor;
                vy *= factor;
            }
            state.getPlayer().setKeyboardVelocity(vx, vy);
        } else {
            state.getPlayer().setKeyboardVelocity(0, 0);
        }
    }

    private void handleBulletFire(double dt, GameState state) {
        bulletCooldown = Math.max(0, bulletCooldown - dt);
        if (state.getPlayer().isInNeutralZone()) {
            input.consumeBulletFire(); // discard
            return;
        }
        boolean bulletInFlight = !state.getPlayerBullets().isEmpty()
                && state.getPlayerBullets().stream().anyMatch(b -> b.isAlive());
        if (input.consumeBulletFire() && bulletCooldown == 0 && !bulletInFlight) {
            double[] dir = state.getPlayer().getFacingDirection();
            double bx = state.getPlayer().getCenterX() + dir[0] * state.getPlayer().getWidth() * 0.5;
            double by = state.getPlayer().getCenterY() + dir[1] * state.getPlayer().getHeight() * 0.5;
            state.addPlayerBullet(new PlayerBullet(bx - PlayerBullet.W / 2.0, by - PlayerBullet.H / 2.0, dir[0], dir[1]));
            bulletCooldown = BULLET_COOLDOWN_SECS;
            state.queueAudio(GameState.AudioEvent.PLAY_BULLET_SHOOT);
        }
    }

    private void handleCannonFire(GameState state) {
        if (state.getPlayer().isInNeutralZone()) {
            input.consumeCannonFire();
            return;
        }
        if (input.consumeCannonFire() && state.isCannonCharged() && state.getZorlonCannon() == null) {
            double startY = state.getPlayer().getCenterY();
            state.setZorlonCannon(new ZorlonCannon(startY));
            state.dischargeCanon();
            state.queueAudio(GameState.AudioEvent.PLAY_CANNON_LAUNCH);
            state.queueAudio(GameState.AudioEvent.START_CANNON_FLY);
        }
    }
}
