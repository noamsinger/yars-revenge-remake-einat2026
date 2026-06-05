package com.yarsrevenge.renderer;

import com.yarsrevenge.model.GameConstants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BackgroundRenderer implements Renderer {

    private static final Color[] RAINBOW = {
        Color.rgb(255,  50,  50),
        Color.rgb(255, 140,  50),
        Color.rgb(255, 255,  50),
        Color.rgb(50,  220,  50),
        Color.rgb(50,  200, 255),
        Color.rgb(80,   80, 255),
        Color.rgb(200,  50, 255),
    };

    // Reduced count and using fillRect instead of fillOval — ~8x fewer Prism draw ops
    private static final int     PARTICLE_COUNT = 350;
    private static final float[] PX    = new float[PARTICLE_COUNT];
    private static final float[] PY    = new float[PARTICLE_COUNT];
    private static final float[] PR    = new float[PARTICLE_COUNT];
    private static final int[]   PC    = new int  [PARTICLE_COUNT];
    private static final float[] PBR   = new float[PARTICLE_COUNT];
    private static final float[] PSP   = new float[PARTICLE_COUNT];

    // Pre-extracted RGB components so we never allocate Color objects in the render loop
    private static final float[] PR_R  = new float[PARTICLE_COUNT];
    private static final float[] PR_G  = new float[PARTICLE_COUNT];
    private static final float[] PR_B  = new float[PARTICLE_COUNT];

    static {
        java.util.Random rng = new java.util.Random(0xDEAD_BEEF);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            PX[i]  = (float)(GameConstants.NZ_LEFT + rng.nextDouble() * GameConstants.NZ_WIDTH);
            PY[i]  = (float)(rng.nextDouble() * GameConstants.LOGICAL_H);
            PR[i]  = (float)(1.5 + rng.nextDouble() * 4.5);
            PC[i]  = rng.nextInt(RAINBOW.length);
            PBR[i] = (float)(0.35 + rng.nextDouble() * 0.65);
            PSP[i] = (float)(1.5  + rng.nextDouble() * 5.0);
            Color base = RAINBOW[PC[i]];
            PR_R[i] = (float) base.getRed();
            PR_G[i] = (float) base.getGreen();
            PR_B[i] = (float) base.getBlue();
        }
    }

    // Bucket colours are recomputed only when the time bucket changes (~6 fps equivalent)
    private static final int BUCKETS = 64; // divides 0..2π evenly enough
    private double lastBucketTime = -1;
    private final Color[][] bucketColors = new Color[PARTICLE_COUNT][2]; // [particle][0=dim,1=bright]

    @Override
    public void render(GraphicsContext gc, RenderContext ctx) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConstants.LOGICAL_W, GameConstants.LOGICAL_H);

        double gt = ctx.playerWingPhase();

        // Quantise time to ~30 steps/sec so we re-allocate Color objects much less often
        double quantised = Math.floor(gt * 30.0) / 30.0;
        boolean recompute = (quantised != lastBucketTime);
        lastBucketTime = quantised;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double flicker = 0.4 + 0.6 * Math.abs(Math.sin(gt * PSP[i] + i * 0.31));
            double bright  = PBR[i] * flicker;

            Color c;
            if (recompute) {
                c = Color.color(
                    Math.min(1.0, PR_R[i] * bright),
                    Math.min(1.0, PR_G[i] * bright),
                    Math.min(1.0, PR_B[i] * bright),
                    Math.min(1.0, bright + 0.08));
                bucketColors[i][0] = c;
            } else {
                c = bucketColors[i][0];
                if (c == null) {
                    c = Color.color(
                        Math.min(1.0, PR_R[i] * bright),
                        Math.min(1.0, PR_G[i] * bright),
                        Math.min(1.0, PR_B[i] * bright),
                        Math.min(1.0, bright + 0.08));
                    bucketColors[i][0] = c;
                }
            }

            gc.setFill(c);
            double r = PR[i];
            gc.fillRect(PX[i] - r, PY[i] - r, r * 2, r * 2);
        }
    }
}
