package com.yarsrevenge.renderer;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;
import com.yarsrevenge.model.entity.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class EntityRenderer implements Renderer {

    // Pre-baked fire particle data — seeded once, reused every frame
    private static final int   F_COUNT  = 180;
    private static final float[] F_ANGLE = new float[F_COUNT];
    private static final float[] F_SPEED = new float[F_COUNT];
    private static final float[] F_LIFE  = new float[F_COUNT];
    private static final float[] F_START = new float[F_COUNT];
    private static final float[] F_SIZE  = new float[F_COUNT];
    private static final float[] F_HUE   = new float[F_COUNT];

    static {
        java.util.Random rng = new java.util.Random(0xF1AE5);
        for (int i = 0; i < F_COUNT; i++) {
            F_ANGLE[i] = (float)(-Math.PI / 2.0 + (rng.nextDouble() - 0.5) * Math.PI * 1.4);
            F_SPEED[i] = (float)(30 + rng.nextDouble() * 90);
            F_LIFE[i]  = (float)(0.3 + rng.nextDouble() * 0.7);
            F_START[i] = (float)(rng.nextDouble() * 0.6);
            F_SIZE[i]  = (float)(4 + rng.nextInt(16));
            F_HUE[i]   = (float)(rng.nextDouble() * 50);
        }
    }

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        drawCannonAimIndicator(gc, ctx);
        drawPlayer(gc, ctx);

        if (ctx.phase() == GameState.Phase.WAVE_TRANSITION) {
            drawQuotileFire(gc, ctx);
        } else {
            com.yarsrevenge.model.entity.Quotile.Mode qMode = ctx.quotileMode();
            if (qMode == com.yarsrevenge.model.entity.Quotile.Mode.MISSILE_WARNING) {
                drawJetOctopus(gc, ctx.quotileX(), ctx.quotileY(), ctx.quotileW(), ctx.playerWingPhase(), 0);
            } else if (qMode != com.yarsrevenge.model.entity.Quotile.Mode.MISSILE_FIRED) {
                drawQuotile(gc, ctx);
            }
        }

        if (ctx.torpedoAlive()) drawTorpedo(gc, ctx);
        drawQuotileShots(gc, ctx);
        drawPlayerBullets(gc, ctx);
        drawCannonBeam(gc, ctx);
        drawSwirl(gc, ctx);
    }

    // ---- player ----

    private void drawPlayer(GraphicsContext gc, RenderContext ctx) {
        if (ctx.phase() == GameState.Phase.PLAYER_DYING) {
            if ((int)(ctx.phaseTimer() * 10) % 2 == 0) return;
        }

        double flapFps = 30.0;
        double t = (ctx.playerWingPhase() * flapFps % FLY_CYCLE) / FLY_CYCLE;
        double cx = ctx.playerX() + ctx.playerW() / 2.0;
        double cy = ctx.playerY() + ctx.playerH() / 2.0;
        double size = ctx.playerW() * 2.2;

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(Math.toDegrees(ctx.playerFacingAngle()));
        FlySprite.draw(gc, 0, 0, size, t);
        gc.restore();
    }

    private static final double FLY_CYCLE = 32.0;

    // ---- quotile ----

    private void drawQuotile(GraphicsContext gc, RenderContext ctx) {
        double animFps = 30.0;
        double t = ((ctx.playerWingPhase() * animFps) % 450.0) / 450.0;
        double cx = ctx.quotileX() + ctx.quotileW() / 2.0;
        double cy = ctx.quotileY() + ctx.quotileH() / 2.0;
        double size = ctx.quotileW() * 2.0;
        QuotileSprite.draw(gc, cx, cy, size, t);
    }

    private void drawQuotileFire(GraphicsContext gc, RenderContext ctx) {
        double t  = ctx.phaseTimer();
        double cx = ctx.quotileX() + ctx.quotileW() / 2.0;
        double cy = ctx.quotileY() + ctx.quotileH() / 2.0;
        double ft = t % 1.0;

        for (int i = 0; i < F_COUNT; i++) {
            double localT = (ft - F_START[i]) / F_LIFE[i];
            if (localT < 0) localT += 1.0 / F_LIFE[i];
            if (localT <= 0 || localT >= 1) continue;
            double dist = localT * F_SPEED[i];
            double px   = cx + Math.cos(F_ANGLE[i]) * dist;
            double py   = cy + Math.sin(F_ANGLE[i]) * dist;
            double size = F_SIZE[i] * (1.0 - localT * 0.8);
            double alpha = (1.0 - localT) * 0.95;
            gc.setFill(Color.hsb(F_HUE[i], 1.0, 1.0, alpha));
            gc.fillOval(px - size / 2, py - size / 2, size, size);
        }

        double glowAlpha = Math.max(0, 1.0 - t / 2.0) * 0.7;
        if (glowAlpha > 0) {
            double glowR = 40 + 20 * Math.sin(t * 8);
            gc.setFill(Color.rgb(200, 0, 0, glowAlpha * 0.30));
            gc.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);
            gc.setFill(Color.rgb(255, 100, 0, glowAlpha * 0.50));
            gc.fillOval(cx - glowR * 0.55, cy - glowR * 0.55, glowR * 1.10, glowR * 1.10);
            gc.setFill(Color.rgb(255, 255, 200, glowAlpha));
            gc.fillOval(cx - glowR * 0.22, cy - glowR * 0.22, glowR * 0.44, glowR * 0.44);
        }
    }

    // ---- torpedo (orb) ----

    private void drawTorpedo(GraphicsContext gc, RenderContext ctx) {
        double animFps = 30.0;
        double t = ((ctx.playerWingPhase() * animFps) % 64.0) / 64.0;
        double cx = ctx.torpedoX() + ctx.torpedoW() / 2.0;
        double cy = ctx.torpedoY() + ctx.torpedoH() / 2.0;
        double size = ctx.torpedoW() * 0.9;
        TorpedoSprite.draw(gc, cx, cy, size, t);
    }

    // ---- quotile shots ----

    private void drawQuotileShots(GraphicsContext gc, RenderContext ctx) {
        gc.setFill(Color.rgb(255, 200, 50));
        for (QuotileShot s : ctx.shots()) {
            if (s.isAlive())
                gc.fillRect(s.getX(), s.getY(), s.getWidth(), s.getHeight());
        }
    }

    private void drawPlayerBullets(GraphicsContext gc, RenderContext ctx) {
        gc.setFill(Color.rgb(180, 255, 100));
        for (PlayerBullet pb : ctx.playerBullets()) {
            if (pb.isAlive())
                gc.fillRect(pb.getX(), pb.getY(), pb.getWidth(), pb.getHeight());
        }
    }

    // ---- cannon ----

    private void drawCannonAimIndicator(GraphicsContext gc, RenderContext ctx) {
        if (!ctx.cannonCharged() || ctx.zorlonCannon() != null || ctx.playerInNZ()) return;
        double beamY = ctx.playerY() + ctx.playerH() / 2.0;
        gc.setFill(Color.rgb(255, 255, 80, 0.9));
        gc.fillRect(0, beamY - 8, 30, 16);
        gc.setStroke(Color.rgb(255, 255, 80, 0.25));
        gc.setLineWidth(2);
        gc.setLineDashes(12, 8);
        gc.strokeLine(30, beamY, GameConstants.LOGICAL_W, beamY);
        gc.setLineDashes(null);
    }

    private void drawCannonBeam(GraphicsContext gc, RenderContext ctx) {
        ZorlonCannon cannon = ctx.zorlonCannon();
        if (cannon == null || !cannon.isAlive()) return;
        gc.setFill(Color.rgb(255, 255, 180));
        gc.fillRect(cannon.getX(), cannon.getY(), cannon.getWidth(), cannon.getHeight());
        gc.setFill(Color.rgb(255, 255, 100, 0.4));
        gc.fillRect(cannon.getX() - 2, cannon.getY() - 4,
                    cannon.getWidth() + 4, cannon.getHeight() + 8);
    }

    // ---- jet octopus ----

    private void drawJetOctopus(GraphicsContext gc, double ex, double ey, double ew,
                                 double wingPhase, double angleDeg) {
        double animFps = 30.0;
        double t = ((wingPhase * animFps) % 60.0) / 60.0;
        double cx   = ex + ew / 2.0;
        double cy   = ey + ew / 2.0;
        double size = ew * 4.0;
        gc.save();
        gc.translate(cx, cy);
        if (angleDeg != 0) gc.rotate(angleDeg);
        JetOctopusSprite.draw(gc, 0, 0, size, t);
        gc.restore();
    }

    // ---- quotile missile ----

    private void drawSwirl(GraphicsContext gc, RenderContext ctx) {
        Swirl m = ctx.swirl();
        if (m == null || !m.isAlive()) return;
        double angleDeg = Math.toDegrees(m.getAngle());
        drawJetOctopus(gc, m.getX(), m.getY(), m.getWidth(), ctx.playerWingPhase(), angleDeg);
    }
}
