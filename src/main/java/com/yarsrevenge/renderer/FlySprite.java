package com.yarsrevenge.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the player fly sprite directly onto a GraphicsContext.
 * No sprite sheet, no snapshot(), no RadialGradient — solid colours only.
 */
public class FlySprite {

    public static void draw(GraphicsContext gc, double cx, double cy, double size, double t) {
        double S = size;
        double ox = cx - S / 2.0;
        double oy = cy - S / 2.0;

        double flapT      = Math.sin(2 * Math.PI * t * 4);
        double wingAng    = flapT * 38.0;
        double bendT      = Math.sin(2 * Math.PI * t * 4 + Math.PI * 0.4);
        double tipBend    = bendT * 18.0;
        double abdomenOff = flapT * 4.0 / 256.0 * S;

        double bcx = ox + S * 0.48;
        double bcy = oy + S * 0.50;

        // Shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.18));
        gc.fillOval(bcx - S * 0.22, bcy + S * 0.26, S * 0.44, S * 0.06);

        drawAbdomen(gc, bcx, bcy, S, abdomenOff);
        drawWings(gc, bcx, bcy, S, wingAng, tipBend);
        drawThorax(gc, bcx, bcy, S);
        drawHead(gc, bcx, bcy, S);
        drawLegs(gc, bcx, bcy, S, flapT);
    }

    private static void drawAbdomen(GraphicsContext gc, double cx, double cy, double S, double wobble) {
        double ax = cx - S * 0.12;
        double ay = cy + wobble;

        // Three-ring body instead of RadialGradient
        gc.setFill(Color.rgb(60, 120, 10));
        gc.fillOval(ax - S * 0.28, ay - S * 0.11, S * 0.38, S * 0.22);
        gc.setFill(Color.rgb(160, 220, 40));
        gc.fillOval(ax - S * 0.22, ay - S * 0.08, S * 0.26, S * 0.16);
        gc.setFill(Color.rgb(200, 255, 80));
        gc.fillOval(ax - S * 0.18, ay - S * 0.05, S * 0.14, S * 0.10);

        gc.setStroke(Color.rgb(40, 90, 5, 0.55));
        gc.setLineWidth(1.5);
        for (int seg = 1; seg <= 4; seg++) {
            double sx = ax - S * 0.28 + seg * (S * 0.38 / 5.0);
            gc.strokeLine(sx, ay - S * 0.11, sx, ay + S * 0.11);
        }

        gc.setFill(Color.rgb(255, 220, 20, 0.45));
        for (int seg = 1; seg <= 2; seg++) {
            double sx = ax - S * 0.28 + seg * (S * 0.38 / 3.5);
            gc.fillRect(sx - 3, ay - S * 0.10, 6, S * 0.20);
        }

        double[] stingerX = {ax - S * 0.28, ax - S * 0.28 - S * 0.07, ax - S * 0.28 - S * 0.03};
        double[] stingerY = {ay - S * 0.03, ay,                        ay + S * 0.03};
        gc.setFill(Color.rgb(80, 130, 10));
        gc.fillPolygon(stingerX, stingerY, 3);
    }

    private static void drawThorax(GraphicsContext gc, double cx, double cy, double S) {
        double tx = cx - S * 0.13;
        double ty = cy - S * 0.12;
        double tw = S * 0.22;
        double th = S * 0.24;
        // Three concentric shapes instead of RadialGradient
        gc.setFill(Color.rgb(120, 180, 20));
        gc.fillRoundRect(tx, ty, tw, th, tw * 0.5, th * 0.5);
        gc.setFill(Color.rgb(190, 235, 60));
        gc.fillRoundRect(tx + tw*0.15, ty + th*0.15, tw*0.70, th*0.70, tw*0.35, th*0.35);
        gc.setFill(Color.rgb(240, 255, 100));
        gc.fillRoundRect(tx + tw*0.32, ty + th*0.20, tw*0.36, th*0.32, tw*0.18, th*0.16);
        gc.setStroke(Color.rgb(80, 130, 10, 0.7));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(tx, ty, tw, th, tw * 0.5, th * 0.5);
        gc.setFill(Color.rgb(255, 255, 200, 0.35));
        gc.fillOval(tx + tw * 0.25, ty + th * 0.12, tw * 0.30, th * 0.22);
    }

    private static void drawHead(GraphicsContext gc, double cx, double cy, double S) {
        double hx = cx + S * 0.10;
        double hy = cy;
        double hr = S * 0.10;
        // Two-ring head instead of RadialGradient
        gc.setFill(Color.rgb(80, 140, 10));
        gc.fillOval(hx - hr, hy - hr, hr * 2, hr * 2);
        gc.setFill(Color.rgb(220, 255, 90));
        gc.fillOval(hx - hr*0.65, hy - hr*0.65, hr*1.30, hr*1.30);
        gc.setStroke(Color.rgb(60, 110, 5, 0.6));
        gc.setLineWidth(1.2);
        gc.strokeOval(hx - hr, hy - hr, hr * 2, hr * 2);

        drawCompoundEye(gc, hx + S * 0.06, hy - S * 0.05, S * 0.07);
        drawCompoundEye(gc, hx + S * 0.06, hy + S * 0.05, S * 0.07);

        gc.setStroke(Color.rgb(100, 160, 20));
        gc.setLineWidth(2.0);
        gc.strokeLine(hx + S * 0.09, hy, hx + S * 0.20, hy + S * 0.02);
        gc.setStroke(Color.rgb(60, 100, 10));
        gc.setLineWidth(1.5);
        gc.strokeLine(hx, hy - S * 0.07, hx + S * 0.08, hy - S * 0.19);
        gc.setFill(Color.rgb(255, 200, 20));
        gc.fillOval(hx + S * 0.06, hy - S * 0.22, S * 0.04, S * 0.04);
        gc.strokeLine(hx, hy + S * 0.07, hx + S * 0.08, hy + S * 0.19);
        gc.fillOval(hx + S * 0.06, hy + S * 0.18, S * 0.04, S * 0.04);
    }

    private static void drawCompoundEye(GraphicsContext gc, double ex, double ey, double r) {
        // Two rings instead of RadialGradient
        gc.setFill(Color.rgb(80, 0, 0));
        gc.fillOval(ex - r, ey - r, r * 2, r * 2);
        gc.setFill(Color.rgb(200, 20, 10));
        gc.fillOval(ex - r*0.65, ey - r*0.65, r*1.30, r*1.30);
        gc.setFill(Color.rgb(255, 80, 60));
        gc.fillOval(ex - r*0.30, ey - r*0.30, r*0.60, r*0.60);
        gc.setStroke(Color.rgb(255, 120, 100, 0.4));
        gc.setLineWidth(0.8);
        for (int fi = 0; fi < 6; fi++) {
            double angle = fi * Math.PI / 3;
            gc.strokeLine(ex, ey, ex + Math.cos(angle) * r * 0.85, ey + Math.sin(angle) * r * 0.85);
        }
        gc.setFill(Color.rgb(255, 255, 255, 0.55));
        gc.fillOval(ex - r * 0.45, ey - r * 0.45, r * 0.4, r * 0.35);
    }

    private static void drawWings(GraphicsContext gc, double cx, double cy, double S,
                                   double wingAngle, double tipBend) {
        double rootX = cx - S * 0.04;
        double rootY = cy;
        Color wingFill  = Color.rgb(180, 245, 255, 0.52);
        Color wingFill2 = Color.rgb(140, 200, 255, 0.38);
        Color wingEdge  = Color.rgb(80, 180, 220, 0.70);
        drawOneWing(gc, rootX, rootY, S, -wingAngle, -tipBend, wingFill, wingFill2, wingEdge, false);
        drawOneWing(gc, rootX, rootY, S,  wingAngle,  tipBend, wingFill, wingFill2, wingEdge, true);
    }

    private static void drawOneWing(GraphicsContext gc, double rootX, double rootY, double S,
                                     double rootAngle, double tipBend,
                                     Color fill, Color fill2, Color edge, boolean lower) {
        double scale   = lower ? 0.78 : 1.0;
        double rootRad = Math.toRadians(rootAngle);
        double wl = S * 0.54 * scale;
        double ww = S * 0.20 * scale;
        double[][] pts = {
            {0,          0},
            {-wl * 0.3,  -ww * 0.6},
            {-wl,        -ww * 0.2 + Math.toRadians(tipBend) * S * 0.12},
            {-wl * 0.85,  ww * 0.5},
            {-wl * 0.4,   ww * 0.8},
            {0,           ww * 0.25},
        };
        double cos = Math.cos(rootRad), sin = Math.sin(rootRad);
        double[] px = new double[pts.length];
        double[] py = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            px[i] = rootX + pts[i][0] * cos - pts[i][1] * sin;
            py[i] = rootY + pts[i][0] * sin + pts[i][1] * cos;
        }
        gc.setFill(fill);
        gc.fillPolygon(px, py, pts.length);
        double[] ipx = new double[pts.length];
        double[] ipy = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            ipx[i] = rootX + pts[i][0] * cos * 0.55 - pts[i][1] * sin * 0.55;
            ipy[i] = rootY + pts[i][0] * sin * 0.55 + pts[i][1] * cos * 0.55;
        }
        gc.setFill(fill2);
        gc.fillPolygon(ipx, ipy, pts.length);
        gc.setStroke(Color.rgb(60, 160, 200, 0.55));
        gc.setLineWidth(0.9);
        gc.strokeLine(px[0], py[0], px[2], py[2]);
        for (int v = 1; v <= 4; v++) {
            double frac = v / 5.0;
            double vx1  = px[0] + (px[2] - px[0]) * frac;
            double vy1  = py[0] + (py[2] - py[0]) * frac;
            double vx2  = px[0] + (px[pts.length - 1] - px[0]) * frac;
            double vy2  = py[0] + (py[pts.length - 1] - py[0]) * frac;
            gc.strokeLine(vx1, vy1, vx1 + (vx2 - vx1) * 1.5, vy1 + (vy2 - vy1) * 1.5);
        }
        gc.setStroke(edge);
        gc.setLineWidth(1.2);
        gc.strokePolygon(px, py, pts.length);
    }

    private static void drawLegs(GraphicsContext gc, double cx, double cy, double S, double flapT) {
        gc.setStroke(Color.rgb(60, 110, 10));
        gc.setLineWidth(1.2);
        double legLift = flapT * S * 0.025;
        double[][] attachPoints = {
            {cx - S * 0.02, cy + S * 0.10},
            {cx - S * 0.07, cy + S * 0.11},
            {cx - S * 0.12, cy + S * 0.10},
        };
        double[][] legTips = {
            {cx + S * 0.08, cy + S * 0.25 - legLift * 1.2},
            {cx - S * 0.02, cy + S * 0.28 - legLift * 0.5},
            {cx - S * 0.14, cy + S * 0.27 + legLift * 0.3},
        };
        for (int i = 0; i < 3; i++) {
            double ax = attachPoints[i][0], ay = attachPoints[i][1];
            double tx = legTips[i][0],      ty = legTips[i][1];
            double kx = (ax + tx) / 2.0 + S * 0.04;
            double ky = (ay + ty) / 2.0 - S * 0.02;
            gc.strokeLine(ax, ay, kx, ky);
            gc.strokeLine(kx, ky, tx, ty);
            gc.strokeLine(tx, ty, tx + S * 0.015, ty + S * 0.018);
            gc.strokeLine(tx, ty, tx + S * 0.020, ty - S * 0.010);
        }
    }
}
