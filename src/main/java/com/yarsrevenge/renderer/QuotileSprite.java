package com.yarsrevenge.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the Quotile (evil octopus facing left) directly onto a GraphicsContext.
 * No sprite sheet, no snapshot(), no RadialGradient — solid colours only.
 */
public class QuotileSprite {

    private static final double[] PALETTE_HUES = {0.0, 55.0, 220.0, 130.0, 290.0};
    private static final int[]    ARM_SPEED    = {1, 2, 3, 1, 4, 2, 3, 1};
    private static final double[] ARM_PHASE    = {0.00, 1.05, 2.09, 3.14, 0.52, 1.57, 3.67, 4.71};

    public static void draw(GraphicsContext gc, double cx, double cy, double size, double t) {
        double S = size;

        double colorPos = t * PALETTE_HUES.length;
        int seg = (int) colorPos % PALETTE_HUES.length;
        int nextSeg = (seg + 1) % PALETTE_HUES.length;
        double blend = colorPos - Math.floor(colorPos);
        blend = (1.0 - Math.cos(blend * Math.PI)) * 0.5;
        double hue = PALETTE_HUES[seg] + (PALETTE_HUES[nextSeg] - PALETTE_HUES[seg]) * blend;
        if (seg == 4 && nextSeg == 0) hue = 290.0 + blend * 70.0;

        double bodyPulse = 0.5 + 0.5 * Math.sin(2 * Math.PI * t * 3);
        Color bodyColor = Color.hsb(hue % 360.0, 0.85, 0.45);
        Color bodyLight = Color.hsb(hue % 360.0, 0.65, 0.70);
        Color bodyDark  = Color.hsb(hue % 360.0, 1.00, 0.20);
        double wave = 2 * Math.PI * t;

        double ox = cx - S / 2.0;
        double oy = cy - S / 2.0;

        double mantleCX = ox + S * 0.68;
        double mantleCY = oy + S * 0.42;
        double mantleRX = S * 0.22;
        double mantleRY = S * 0.26 * (1.0 + 0.04 * bodyPulse);

        drawArms(gc, S, ox, oy, mantleCX, mantleCY, wave, bodyColor, bodyDark);

        // Three concentric ovals instead of RadialGradient
        gc.setFill(bodyDark);
        gc.fillOval(mantleCX - mantleRX, mantleCY - mantleRY, mantleRX * 2, mantleRY * 2);
        gc.setFill(bodyColor);
        gc.fillOval(mantleCX - mantleRX * 0.72, mantleCY - mantleRY * 0.72,
                    mantleRX * 1.44, mantleRY * 1.44);
        gc.setFill(bodyLight);
        gc.fillOval(mantleCX - mantleRX * 0.38, mantleCY - mantleRY * 0.45,
                    mantleRX * 0.76, mantleRY * 0.70);

        gc.setStroke(bodyDark.deriveColor(0, 1, 0.6, 0.5));
        gc.setLineWidth(0.7);
        for (int line = 1; line <= 3; line++) {
            double lf = line / 4.0;
            gc.strokeOval(
                mantleCX - mantleRX * lf, mantleCY - mantleRY * lf,
                mantleRX * lf * 2, mantleRY * lf * 2);
        }

        drawEye(gc, mantleCX - mantleRX * 0.35, mantleCY - mantleRY * 0.25, S * 0.085, t);
        drawEye(gc, mantleCX - mantleRX * 0.35, mantleCY + mantleRY * 0.25, S * 0.085, t);

        double[] tipX = {mantleCX + mantleRX * 0.6, mantleCX + mantleRX * 1.1, mantleCX + mantleRX * 0.6};
        double[] tipY = {mantleCY - mantleRY * 0.5, mantleCY, mantleCY + mantleRY * 0.5};
        gc.setFill(bodyColor);
        gc.fillPolygon(tipX, tipY, 3);

        // Specular highlight — solid white oval instead of RadialGradient
        gc.setFill(Color.rgb(255, 255, 255, 0.30));
        gc.fillOval(mantleCX - mantleRX * 0.55, mantleCY - mantleRY * 0.75,
                    mantleRX * 0.8, mantleRY * 0.5);
    }

    private static void drawArms(GraphicsContext gc, double S, double ox, double oy,
                                  double mx, double my,
                                  double wave, Color base, Color dark) {
        int armCount = 8;
        int segs = 7;
        double armLen   = S * 0.40;
        double tipCircR = S * 0.13;

        for (int a = 0; a < armCount; a++) {
            double vertFrac = (a / (double)(armCount - 1)) - 0.5;
            double rootY    = my + vertFrac * S * 0.44;
            double rootX    = mx - S * 0.18;

            double tipAngle = wave * ARM_SPEED[a] + ARM_PHASE[a];
            double tipCX = rootX - armLen * 0.85;
            double tipCY = rootY;
            double tipX  = tipCX + Math.cos(tipAngle) * tipCircR;
            double tipY  = tipCY + Math.sin(tipAngle) * tipCircR;

            double[] px = new double[segs * 2];
            double[] py = new double[segs * 2];

            for (int s = 0; s < segs; s++) {
                double frac = s / (double)(segs - 1);
                double segX = rootX + (tipX - rootX) * frac;
                double segY = rootY + (tipY - rootY) * frac;
                double bulge = Math.sin(frac * Math.PI) * S * 0.04;
                double dx = tipX - rootX, dy = tipY - rootY;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 0.01) { segX += (-dy / len) * bulge; segY += (dx / len) * bulge; }
                double halfW = S * 0.048 * (1.0 - frac * 0.80);
                double nx = -dy / (len + 0.001), ny = dx / (len + 0.001);
                px[s]            = segX + nx * halfW;
                py[s]            = segY + ny * halfW;
                px[segs*2-1 - s] = segX - nx * halfW;
                py[segs*2-1 - s] = segY - ny * halfW;
            }

            double alpha = 0.85 + 0.12 * Math.sin(tipAngle);
            gc.setFill(base.deriveColor(0, 1, 1, alpha));
            gc.fillPolygon(px, py, segs * 2);

            gc.setFill(dark.deriveColor(0, 1, 0.7, 0.6));
            for (int s = 1; s < segs - 1; s++) {
                double frac = s / (double)(segs - 1);
                double segX = rootX + (tipX - rootX) * frac;
                double segY = rootY + (tipY - rootY) * frac;
                double sr   = S * 0.018 * (1.0 - frac * 0.5);
                gc.fillOval(segX - sr, segY - sr, sr * 2, sr * 2);
            }

            gc.setStroke(dark.deriveColor(0, 1, 0.8, 0.55));
            gc.setLineWidth(0.6);
            gc.strokePolygon(px, py, segs * 2);
        }
    }

    private static void drawEye(GraphicsContext gc, double ex, double ey, double r, double t) {
        double pulse = 0.5 + 0.5 * Math.sin(2 * Math.PI * t * 2);
        gc.setFill(Color.rgb(220, 220, 180));
        gc.fillOval(ex - r, ey - r, r * 2, r * 2);
        int ir = (int)(200 + 55 * pulse);
        int ig = (int)(20  + 60 * pulse);
        gc.setFill(Color.rgb(ir, ig, 0));
        gc.fillOval(ex - r * 0.55, ey - r * 0.55, r * 1.1, r * 1.1);
        gc.setFill(Color.rgb(5, 0, 0));
        gc.fillOval(ex - r * 0.15, ey - r * 0.75, r * 0.30, r * 1.50);
        gc.setStroke(Color.rgb(40, 0, 0, 0.8));
        gc.setLineWidth(0.8);
        gc.strokeOval(ex - r, ey - r, r * 2, r * 2);
        gc.setFill(Color.rgb(255, 255, 255, 0.55));
        gc.fillOval(ex - r * 0.50, ey - r * 0.60, r * 0.38, r * 0.28);
    }
}
