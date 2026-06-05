package com.yarsrevenge.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws the Swirl (energy ball) directly onto a GraphicsContext.
 * No sprite sheet, no snapshot(), no RadialGradient — solid colours only.
 */
public class SwirlSprite {

    public static void draw(GraphicsContext gc, double cx, double cy, double size, double t) {
        double S = size;
        double rot   = 2 * Math.PI * t;
        double pulse = 0.5 + 0.5 * Math.sin(2 * Math.PI * t);
        double ballR = S * 0.40;

        // Core ball — three concentric ovals instead of RadialGradient
        gc.setFill(Color.rgb(0, 20, 40));
        gc.fillOval(cx - ballR, cy - ballR, ballR * 2, ballR * 2);
        gc.setFill(Color.rgb(0, 80, 110));
        gc.fillOval(cx - ballR * 0.72, cy - ballR * 0.72, ballR * 1.44, ballR * 1.44);
        gc.setFill(Color.rgb(20, 160, 180));
        gc.fillOval(cx - ballR * 0.42, cy - ballR * 0.42, ballR * 0.84, ballR * 0.84);

        // Hot spot A — rotating bright core
        double hotDist = ballR * 0.38;
        double haxA = cx + Math.cos(rot) * hotDist;
        double hayA = cy + Math.sin(rot) * hotDist;
        double hotRA = ballR * (0.52 + 0.12 * pulse);
        gc.setFill(Color.rgb(0, 180, 220, 0.40 + 0.20 * pulse));
        gc.fillOval(haxA - hotRA, hayA - hotRA, hotRA * 2, hotRA * 2);
        double coreRA = hotRA * 0.45;
        gc.setFill(Color.rgb(180, 255, 255, 0.65 + 0.20 * pulse));
        gc.fillOval(haxA - coreRA, hayA - coreRA, coreRA * 2, coreRA * 2);

        // Hot spot B — counter-rotating secondary core
        double haxB = cx + Math.cos(-rot * 0.7 + Math.PI * 0.6) * hotDist * 0.7;
        double hayB = cy + Math.sin(-rot * 0.7 + Math.PI * 0.6) * hotDist * 0.7;
        double hotRB = ballR * (0.38 + 0.08 * pulse);
        gc.setFill(Color.rgb(0, 140, 200, 0.35 + 0.15 * pulse));
        gc.fillOval(haxB - hotRB, hayB - hotRB, hotRB * 2, hotRB * 2);
        double coreRB = hotRB * 0.40;
        gc.setFill(Color.rgb(120, 220, 255, 0.50 + 0.15 * pulse));
        gc.fillOval(haxB - coreRB, hayB - coreRB, coreRB * 2, coreRB * 2);

        // Equatorial ring stroke
        double ringOffX = Math.cos(rot * 0.5) * ballR * 0.08;
        gc.setStroke(Color.rgb(0, 230, 255, 0.30 + 0.20 * pulse));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - ballR * 0.80 + ringOffX, cy - ballR * 0.18,
                      ballR * 1.60, ballR * 0.36);

        // Halo ring (thin outer glow)
        double haloR = ballR * 1.18;
        gc.setStroke(Color.rgb(0, 200, 255, 0.10 + 0.08 * pulse));
        gc.setLineWidth(ballR * 0.14);
        gc.strokeOval(cx - haloR, cy - haloR, haloR * 2, haloR * 2);

        // Centre pin highlight
        double pinR = ballR * (0.10 + 0.04 * pulse);
        gc.setFill(Color.rgb(200, 255, 255, 0.70));
        gc.fillOval(cx - pinR, cy - pinR, pinR * 2, pinR * 2);
        gc.setFill(Color.rgb(255, 255, 255, 0.90));
        gc.fillOval(cx - pinR * 0.50, cy - pinR * 0.50, pinR, pinR);

        // Specular highlight
        double specR = ballR * 0.22;
        gc.setFill(Color.rgb(255, 255, 255, 0.45));
        gc.fillOval(cx - ballR * 0.28 - specR * 0.7, cy - ballR * 0.28 - specR * 0.7,
                    specR * 1.4, specR * 1.4);
    }
}
