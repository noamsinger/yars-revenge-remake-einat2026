package com.yarsrevenge.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the JetOctopus (flying enemy / missile) directly onto a GraphicsContext.
 * No sprite sheet, no snapshot(), no RadialGradient — solid colours only so
 * the Prism shader cache is never cold when this sprite first appears.
 */
public class JetOctopusSprite {

    private static final double   BODY_SPINS = 4.0;
    private static final int[]    ARM_SPEED  = {1, 3, 5, 2, 7, 4, 6, 2};
    private static final double[] ARM_PHASE  = {
        0.00, 0.785, 1.571, 2.356, 3.142, 3.927, 4.712, 5.498
    };
    private static final double[] ARM_ANCHOR = new double[8];
    static {
        for (int a = 0; a < 8; a++)
            ARM_ANCHOR[a] = a * 2 * Math.PI / 8.0;
    }

    public static void draw(GraphicsContext gc, double cx, double cy, double size, double t) {
        double S = size;

        double bodyAngle = 2 * Math.PI * BODY_SPINS * t;
        double hue       = (t * 720.0) % 360.0;

        Color bodyColor = Color.hsb(hue, 0.90, 0.55);
        Color bodyLight = Color.hsb(hue, 0.60, 0.80);
        Color bodyDark  = Color.hsb(hue, 1.00, 0.25);

        double pulse = 0.5 + 0.5 * Math.sin(2 * Math.PI * t * 6);
        double bodyR  = S * 0.09 * (1.0 + 0.05 * pulse);

        // Thruster glow — concentric circles, no gradient
        for (int streak = 0; streak < 3; streak++) {
            double trailAngle = bodyAngle - (streak + 1) * 0.35;
            double trailDist  = S * 0.10 * (streak + 1);
            double tx     = cx + Math.cos(trailAngle) * trailDist;
            double ty     = cy + Math.sin(trailAngle) * trailDist;
            double trailR = S * 0.14 / (streak + 1);
            double alpha  = 0.30 / (streak + 1);
            gc.setFill(bodyColor.deriveColor(0, 1, 1.4, alpha * 2));
            gc.fillOval(tx - trailR, ty - trailR, trailR * 2, trailR * 2);
            // inner bright core
            double coreR = trailR * 0.45;
            gc.setFill(bodyLight.deriveColor(0, 1, 1, alpha * 1.2));
            gc.fillOval(tx - coreR, ty - coreR, coreR * 2, coreR * 2);
        }

        drawSpiralArms(gc, S, cx, cy, bodyAngle, t, bodyColor, bodyDark);

        // Body — three concentric circles instead of RadialGradient
        gc.setFill(bodyDark);
        gc.fillOval(cx - bodyR, cy - bodyR, bodyR * 2, bodyR * 2);
        double midR = bodyR * 0.72;
        gc.setFill(bodyColor);
        gc.fillOval(cx - midR, cy - midR, midR * 2, midR * 2);
        double hiR = bodyR * 0.38;
        gc.setFill(bodyLight);
        gc.fillOval(cx - hiR - bodyR * 0.20, cy - hiR - bodyR * 0.20, hiR * 2, hiR * 2);

        drawMantleTip(gc, cx, cy, bodyR, bodyAngle, bodyColor);
        drawEyes(gc, cx, cy, bodyR, bodyAngle, t);

        // Specular highlight — small white circle, no gradient
        double specR = bodyR * 0.22;
        gc.setFill(Color.rgb(255, 255, 255, 0.38));
        gc.fillOval(cx - bodyR * 0.22 - specR, cy - bodyR * 0.25 - specR, specR * 2, specR * 2);
    }

    private static void drawSpiralArms(GraphicsContext gc, double S,
                                        double cx, double cy,
                                        double bodyAngle, double t,
                                        Color base, Color dark) {
        int segs = 8;
        for (int a = 0; a < 8; a++) {
            double anchorAngle = ARM_ANCHOR[a] + bodyAngle;
            double armRootDist = S * 0.10;
            double rootX = cx + Math.cos(anchorAngle) * armRootDist;
            double rootY = cy + Math.sin(anchorAngle) * armRootDist;

            double spiralAngle = anchorAngle + 2 * Math.PI * ARM_SPEED[a] * t + ARM_PHASE[a];
            double spiralR = S * 0.46 * (0.5 + 0.5 * Math.abs(
                Math.sin(Math.PI * ARM_SPEED[a] * t + ARM_PHASE[a] * 0.5)));
            double tipX = cx + Math.cos(spiralAngle) * spiralR;
            double tipY = cy + Math.sin(spiralAngle) * spiralR;

            double[] px = new double[segs * 2];
            double[] py = new double[segs * 2];

            double dx = tipX - rootX, dy = tipY - rootY;
            double len = Math.sqrt(dx * dx + dy * dy);

            for (int s = 0; s < segs; s++) {
                double frac = s / (double)(segs - 1);
                double segX = rootX + dx * frac;
                double segY = rootY + dy * frac;

                double bulge = Math.sin(frac * Math.PI) * S * 0.05;
                if (len > 0.01) { segX += (-dy / len) * bulge; segY += (dx / len) * bulge; }

                double halfW = S * 0.028 * (1.0 - frac * 0.82);
                double nx = (len > 0.01) ? -dy / len : 0;
                double ny = (len > 0.01) ?  dx / len : 1;
                px[s]            = segX + nx * halfW;
                py[s]            = segY + ny * halfW;
                px[segs*2-1 - s] = segX - nx * halfW;
                py[segs*2-1 - s] = segY - ny * halfW;
            }

            gc.setFill(base.deriveColor(0, 1, 1, 0.88));
            gc.fillPolygon(px, py, segs * 2);

            gc.setFill(dark.deriveColor(0, 1, 0.7, 0.55));
            for (int s = 1; s < segs - 1; s++) {
                double frac = s / (double)(segs - 1);
                double segX = rootX + dx * frac;
                double segY = rootY + dy * frac;
                double sr = S * 0.014 * (1.0 - frac * 0.5);
                gc.fillOval(segX - sr, segY - sr, sr * 2, sr * 2);
            }

            gc.setStroke(dark.deriveColor(0, 1, 0.8, 0.50));
            gc.setLineWidth(0.5);
            gc.strokePolygon(px, py, segs * 2);
        }
    }

    private static void drawMantleTip(GraphicsContext gc, double cx, double cy,
                                       double bodyR, double bodyAngle, Color bodyColor) {
        double finLen = bodyR * 1.3;
        double[] tipX = {
            cx + Math.cos(bodyAngle) * bodyR * 0.7,
            cx + Math.cos(bodyAngle) * (bodyR + finLen),
            cx + Math.cos(bodyAngle + 0.4) * bodyR * 0.7
        };
        double[] tipY = {
            cy + Math.sin(bodyAngle) * bodyR * 0.7,
            cy + Math.sin(bodyAngle) * (bodyR + finLen),
            cy + Math.sin(bodyAngle + 0.4) * bodyR * 0.7
        };
        gc.setFill(bodyColor.deriveColor(0, 1, 0.8, 0.85));
        gc.fillPolygon(tipX, tipY, 3);
    }

    private static void drawEyes(GraphicsContext gc, double cx, double cy,
                                  double bodyR, double bodyAngle, double t) {
        double eyeOffset = bodyR * 0.45;
        double eyeSpread = 0.45;
        for (int e = 0; e < 2; e++) {
            double eyeAngle = bodyAngle + (e == 0 ? eyeSpread : -eyeSpread);
            double ex = cx + Math.cos(eyeAngle) * eyeOffset;
            double ey = cy + Math.sin(eyeAngle) * eyeOffset;
            double r  = bodyR * 0.22;

            gc.setFill(Color.rgb(230, 230, 190));
            gc.fillOval(ex - r, ey - r, r * 2, r * 2);

            double p = 0.5 + 0.5 * Math.sin(2 * Math.PI * t * 3);
            gc.setFill(Color.rgb((int)(190 + 65 * p), (int)(15 + 50 * p), 0));
            gc.fillOval(ex - r * 0.55, ey - r * 0.55, r * 1.1, r * 1.1);

            gc.setFill(Color.rgb(5, 0, 0));
            gc.fillOval(ex - r * 0.14, ey - r * 0.72, r * 0.28, r * 1.44);

            gc.setStroke(Color.rgb(40, 0, 0, 0.8));
            gc.setLineWidth(0.7);
            gc.strokeOval(ex - r, ey - r, r * 2, r * 2);

            gc.setFill(Color.rgb(255, 255, 255, 0.55));
            gc.fillOval(ex - r * 0.48, ey - r * 0.55, r * 0.35, r * 0.26);
        }
    }
}
