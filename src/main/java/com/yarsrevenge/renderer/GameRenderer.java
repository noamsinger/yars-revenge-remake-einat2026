package com.yarsrevenge.renderer;

import com.yarsrevenge.model.GameConstants;
import com.yarsrevenge.model.GameState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GameRenderer {

    private final BackgroundRenderer bgRenderer    = new BackgroundRenderer();
    private final ShieldRenderer     shieldRenderer = new ShieldRenderer();
    private final EntityRenderer     entityRenderer = new EntityRenderer();
    private final HudRenderer        hudRenderer    = new HudRenderer();

    private double physW;
    private double physH;

    // Letterbox draw region (within the full canvas)
    private double drawX = 0;
    private double drawY = 0;
    private double drawW = -1; // -1 = fill entire canvas
    private double drawH = -1;

    private boolean warmupDone = false;

    // Pre-baked explosion particle data — computed once, reused every frame
    private static final int   P_COUNT   = 300;
    private static final int   S_COUNT   = 32;
    private static final int   E_COUNT   = 120;
    private static final float[] P_ANGLE  = new float[P_COUNT];
    private static final float[] P_SPEED  = new float[P_COUNT];
    private static final float[] P_LIFE   = new float[P_COUNT];
    private static final float[] P_START  = new float[P_COUNT];
    private static final float[] P_SIZE   = new float[P_COUNT];
    private static final float[] P_HUE    = new float[P_COUNT];
    private static final float[] S_ANGLE  = new float[S_COUNT];
    private static final float[] S_START  = new float[S_COUNT];
    private static final float[] S_LIFE   = new float[S_COUNT];
    private static final float[] S_LEN    = new float[S_COUNT];
    private static final float[] S_WIDTH  = new float[S_COUNT];
    private static final float[] S_HUE    = new float[S_COUNT];
    private static final float[] E_ANGLE  = new float[E_COUNT];
    private static final float[] E_DIST   = new float[E_COUNT];
    private static final float[] E_START  = new float[E_COUNT];
    private static final float[] E_LIFE   = new float[E_COUNT];
    private static final float[] E_SIZE   = new float[E_COUNT];
    private static final float[] E_HUE    = new float[E_COUNT];

    static {
        java.util.Random rng = new java.util.Random(0xEA7B00B5L);
        double W = GameConstants.LOGICAL_W;
        for (int i = 0; i < P_COUNT; i++) {
            P_ANGLE[i] = (float)(rng.nextDouble() * Math.PI * 2);
            P_SPEED[i] = (float)(0.20 + rng.nextDouble() * 0.80);
            P_LIFE[i]  = (float)(0.35 + rng.nextDouble() * 0.65);
            P_START[i] = (float)(rng.nextDouble() * 0.35);
            P_SIZE[i]  = (float)(3 + rng.nextInt(14));
            P_HUE[i]   = (float)(rng.nextDouble() * 60);
        }
        rng = new java.util.Random(0x57A4557L);
        for (int i = 0; i < S_COUNT; i++) {
            S_ANGLE[i] = (float)((i / (double) S_COUNT) * Math.PI * 2 + rng.nextDouble() * 0.3);
            S_START[i] = (float)(rng.nextDouble() * 0.25);
            S_LIFE[i]  = (float)(0.4 + rng.nextDouble() * 0.6);
            S_LEN[i]   = (float)(80 + rng.nextDouble() * 120);
            S_WIDTH[i] = (float)(1.5 + rng.nextDouble() * 3);
            S_HUE[i]   = (float)(20 + rng.nextDouble() * 50);
        }
        rng = new java.util.Random(0xEB3A77L);
        for (int i = 0; i < E_COUNT; i++) {
            E_ANGLE[i] = (float)(rng.nextDouble() * Math.PI * 2);
            E_DIST[i]  = (float)(80 + rng.nextDouble() * W * 0.45);
            E_START[i] = (float)(rng.nextDouble() * 0.5);
            E_LIFE[i]  = (float)(0.5 + rng.nextDouble() * 0.5);
            E_SIZE[i]  = (float)(2 + rng.nextInt(5));
            E_HUE[i]   = (float)(10 + rng.nextDouble() * 40);
        }
    }

    public void setPhysicalSize(double w, double h) {
        this.physW = w;
        this.physH = h;
        this.drawX = 0;
        this.drawY = 0;
        this.drawW = w;
        this.drawH = h;
    }

    /** Set a sub-region to draw into (letterboxing). Call after setPhysicalSize. */
    public void setDrawRegion(double x, double y, double w, double h) {
        this.drawX = x;
        this.drawY = y;
        this.drawW = w;
        this.drawH = h;
    }

    public void render(GraphicsContext gc, com.yarsrevenge.model.GameState state) {
        RenderContext ctx = RenderContext.snapshot(state);

        // Fill full canvas black (letterbox bars)
        gc.clearRect(0, 0, physW, physH);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, physW, physH);

        gc.save();
        gc.translate(drawX, drawY);
        gc.scale(drawW / GameConstants.LOGICAL_W, drawH / GameConstants.LOGICAL_H);

        // First-frame GPU warmup: draw all gradient-heavy sprites at alpha=0 so
        // Prism compiles their shaders now, not when the alarm first fires.
        if (!warmupDone) {
            warmupDone = true;
            gc.save();
            gc.setGlobalAlpha(0.0);
            JetOctopusSprite.draw(gc, -500, -500, 100, 0.0);
            JetOctopusSprite.draw(gc, -500, -500, 100, 0.5);
            QuotileSprite.draw(gc, -500, -500, 100, 0.0);
            FlySprite.draw(gc, -500, -500, 100, 0.0);
            SwirlSprite.draw(gc, -500, -500, 100, 0.0);
            gc.restore();
        }

        bgRenderer.render(gc, ctx);
        shieldRenderer.render(gc, ctx);
        entityRenderer.render(gc, ctx);
        hudRenderer.render(gc, ctx);

        renderPhaseOverlay(gc, ctx);

        gc.restore();
    }

    private void drawQuotileExplosion(GraphicsContext gc, double t, double epicX, double epicY) {
        double W = GameConstants.LOGICAL_W;
        double H = GameConstants.LOGICAL_H;

        // Phase 1 (0–0.2s): blinding white flash
        if (t < 0.20) {
            double a = 1.0 - t / 0.20;
            gc.setFill(Color.rgb(255, 255, 255, a));
            gc.fillRect(0, 0, W, H);
        }

        // Phase 2 (0.05–2.5s): shockwave rings + main particle burst + fireball
        if (t >= 0.05 && t < 2.5) {
            double pt = Math.min(1.0, (t - 0.05) / 2.45);

            Color[] ringColors = {
                Color.rgb(255,255,200), Color.rgb(255,200,80), Color.rgb(255,120,20),
                Color.rgb(220,50,10),   Color.rgb(160,20,5),   Color.rgb(255,255,80),
                Color.rgb(200,80,200)
            };
            for (int r = 0; r < ringColors.length; r++) {
                double ringT = Math.max(0, pt - r * 0.07);
                if (ringT <= 0) continue;
                double radius = ringT * W * 0.90;
                double alpha  = Math.max(0, 1.0 - ringT * 1.2);
                gc.setStroke(ringColors[r].deriveColor(0, 1, 1, alpha));
                gc.setLineWidth(Math.max(1, 20 * (1.0 - ringT)));
                gc.strokeOval(epicX - radius, epicY - radius, radius * 2, radius * 2);
            }

            for (int i = 0; i < P_COUNT; i++) {
                double localT = (pt - P_START[i]) / P_LIFE[i];
                if (localT <= 0 || localT >= 1) continue;
                double dist = localT * P_SPEED[i] * W * 0.65;
                double px   = epicX + Math.cos(P_ANGLE[i]) * dist;
                double py   = epicY + Math.sin(P_ANGLE[i]) * dist;
                double size = P_SIZE[i] * (1.0 - localT * 0.75);
                gc.setFill(Color.hsb(P_HUE[i], 1.0, 1.0, 1.0 - localT));
                gc.fillRect(px - size / 2, py - size / 2, size, size);
            }

            double fbRadius = Math.max(0, (1.0 - pt * 1.2) * 350);
            if (fbRadius > 0) {
                gc.setFill(new javafx.scene.paint.RadialGradient(0, 0,
                    epicX, epicY, fbRadius, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.0, Color.rgb(255,255,200,0.95)),
                    new javafx.scene.paint.Stop(0.4, Color.rgb(255,140,20,0.80)),
                    new javafx.scene.paint.Stop(1.0, Color.rgb(180,20,0,0.0))));
                gc.fillOval(epicX - fbRadius, epicY - fbRadius, fbRadius * 2, fbRadius * 2);
            }
        }

        // Phase 3 (0.8–3.5s): starburst streaks
        if (t >= 0.8 && t < 3.5) {
            double st = (t - 0.8) / 2.7;
            for (int i = 0; i < S_COUNT; i++) {
                double localT = (st - S_START[i]) / S_LIFE[i];
                if (localT <= 0 || localT >= 1) continue;
                double r1    = localT * W * 0.55;
                double r2    = r1 + S_LEN[i];
                double alpha = (1.0 - localT) * 0.8;
                gc.setStroke(Color.hsb(S_HUE[i], 1.0, 1.0, alpha));
                gc.setLineWidth(S_WIDTH[i]);
                gc.strokeLine(epicX + Math.cos(S_ANGLE[i]) * r1, epicY + Math.sin(S_ANGLE[i]) * r1,
                              epicX + Math.cos(S_ANGLE[i]) * r2, epicY + Math.sin(S_ANGLE[i]) * r2);
            }
        }

        // Phase 4 (1.5–4.5s): colour-cycling screen tint + second ring
        if (t >= 1.5 && t < 4.5) {
            double pt2  = (t - 1.5) / 3.0;
            double tint = Math.max(0, 0.25 * (1.0 - pt2));
            gc.setFill(Color.hsb(20 * pt2, 1.0, 0.8, tint));
            gc.fillRect(0, 0, W, H);

            double ringT  = Math.min(1.0, pt2 * 1.4);
            double radius = ringT * W * 0.70;
            double alpha  = Math.max(0, 1.0 - ringT * 1.4);
            gc.setStroke(Color.rgb(255, 200, 80, alpha));
            gc.setLineWidth(Math.max(1, 12 * (1.0 - ringT)));
            gc.strokeOval(epicX - radius, epicY - radius, radius * 2, radius * 2);
        }

        // Phase 5 (2.5–5.0s): slow ember drift
        if (t >= 2.5) {
            double et = Math.min(1.0, (t - 2.5) / 2.5);
            for (int i = 0; i < E_COUNT; i++) {
                double localT = (et - E_START[i]) / E_LIFE[i];
                if (localT <= 0 || localT >= 1) continue;
                double drift = localT * E_DIST[i];
                double px    = epicX + Math.cos(E_ANGLE[i]) * drift;
                double py    = epicY + Math.sin(E_ANGLE[i]) * drift + localT * localT * 60;
                double size  = E_SIZE[i] * (1.0 - localT);
                gc.setFill(Color.hsb(E_HUE[i], 1.0, 1.0, (1.0 - localT) * 0.9));
                gc.fillOval(px - size / 2, py - size / 2, size, size);
            }
        }
    }

    private void renderPhaseOverlay(GraphicsContext gc, RenderContext ctx) {
        switch (ctx.phase()) {
            case PAUSED -> {
                gc.setFill(Color.rgb(0, 0, 0, 0.72));
                gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

                gc.setFont(com.yarsrevenge.renderer.GameFont.of(60));
                gc.setFill(Color.rgb(255, 220, 50));
                gc.fillText("PAUSED", 760, 380);

                gc.setFont(com.yarsrevenge.renderer.GameFont.of(36));

                boolean resumeSel = ctx.pauseSelection() == 0;
                gc.setFill(resumeSel ? Color.rgb(255, 255, 80) : Color.rgb(180, 180, 180));
                gc.fillText((resumeSel ? "> " : "  ") + "RESUME", 780, 510);

                boolean quitSel = ctx.pauseSelection() == 1;
                gc.setFill(quitSel ? Color.rgb(255, 100, 80) : Color.rgb(180, 180, 180));
                gc.fillText((quitSel ? "> " : "  ") + "QUIT TO MENU", 720, 600);

                gc.setFont(com.yarsrevenge.renderer.GameFont.of(18));
                gc.setFill(Color.rgb(140, 140, 140));
                gc.fillText("UP/DOWN SELECT   ENTER CONFIRM   ESC RESUME", 540, 700);
            }
            case WAVE_TRANSITION -> {
                double t = ctx.phaseTimer();
                drawQuotileExplosion(gc, t, ctx.explosionX(), ctx.explosionY());

                double textAlpha = Math.max(0, (t - 3.5) / 1.5);
                if (textAlpha > 0) {
                    gc.setFill(Color.rgb(0, 0, 0, Math.min(0.7, textAlpha * 0.7)));
                    gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);
                    gc.setFill(Color.rgb(255, 220, 50, textAlpha));
                    gc.setFont(com.yarsrevenge.renderer.GameFont.of(54));
                    String msg = "WAVE " + ctx.wave() + " CLEAR!";
                    gc.fillText(msg,
                        GameConstants.LOGICAL_W / 2.0 - msg.length() * 15,
                        GameConstants.LOGICAL_H / 2.0);
                }
            }
            case PLAYER_DYING -> {
                double alpha = 1.0 - (ctx.phaseTimer() / GameConstants.PLAYER_DYING_SECS);
                if (alpha > 0) {
                    gc.setFill(Color.rgb(255, 255, 255, alpha * 0.6));
                    gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);
                }
            }
            default -> {}
        }
    }
}
