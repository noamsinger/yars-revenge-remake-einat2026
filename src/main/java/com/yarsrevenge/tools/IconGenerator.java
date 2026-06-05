package com.yarsrevenge.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Run once to generate src/main/resources/icon.png (512×512).
 * A fly face looking straight into the camera: large compound eyes, fuzzy head,
 * antennae, proboscis — on a transparent background.
 */
public class IconGenerator {

    public static void main(String[] args) throws Exception {
        int S = 512;
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double cx = S * 0.5;
        double cy = S * 0.52;

        // ---- transparent background ----
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, S, S);
        g.setComposite(AlphaComposite.SrcOver);

        // ---- fuzzy head halo (back-most layer) ----
        drawRadial(g, cx, cy, S * 0.38, S * 0.42,
                new Color(100, 160, 20, 60), new Color(60, 100, 10, 0));

        // ---- antennae ----
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(60, 110, 10));
        // left antenna
        g.draw(new Line2D.Double(cx - S*0.12, cy - S*0.30, cx - S*0.22, cy - S*0.52));
        g.draw(new Line2D.Double(cx - S*0.03, cy - S*0.31, cx - S*0.04, cy - S*0.54));
        // right antenna
        g.draw(new Line2D.Double(cx + S*0.12, cy - S*0.30, cx + S*0.22, cy - S*0.52));
        g.draw(new Line2D.Double(cx + S*0.03, cy - S*0.31, cx + S*0.04, cy - S*0.54));
        // antenna knobs
        g.setColor(new Color(255, 200, 20));
        fillCircle(g, cx - S*0.22, cy - S*0.54, S*0.025);
        fillCircle(g, cx - S*0.04, cy - S*0.56, S*0.022);
        fillCircle(g, cx + S*0.22, cy - S*0.54, S*0.025);
        fillCircle(g, cx + S*0.04, cy - S*0.56, S*0.022);

        // ---- wings (top sides, partially behind head) ----
        g.setColor(new Color(140, 220, 255, 120));
        // left wing
        fillPoly(g, new double[]{cx-S*.08, cx-S*.42, cx-S*.48, cx-S*.18},
                    new double[]{cy-S*.18, cy-S*.38, cy-S*.10, cy+S*.05});
        // right wing
        fillPoly(g, new double[]{cx+S*.08, cx+S*.42, cx+S*.48, cx+S*.18},
                    new double[]{cy-S*.18, cy-S*.38, cy-S*.10, cy+S*.05});
        // wing vein outline
        g.setColor(new Color(80, 180, 220, 150));
        g.setStroke(new BasicStroke(1.5f));
        drawPoly(g, new double[]{cx-S*.08, cx-S*.42, cx-S*.48, cx-S*.18},
                    new double[]{cy-S*.18, cy-S*.38, cy-S*.10, cy+S*.05});
        drawPoly(g, new double[]{cx+S*.08, cx+S*.42, cx+S*.48, cx+S*.18},
                    new double[]{cy-S*.18, cy-S*.38, cy-S*.10, cy+S*.05});

        // ---- head sphere ----
        // Base green gradient
        GradientPaint headGrad = new GradientPaint(
                (float)(cx - S*0.25), (float)(cy - S*0.32),
                new Color(210, 255, 80),
                (float)(cx + S*0.20), (float)(cy + S*0.28),
                new Color(80, 140, 15));
        g.setPaint(headGrad);
        g.fill(new Ellipse2D.Double(cx - S*0.34, cy - S*0.34, S*0.68, S*0.68));

        // Head outline
        g.setColor(new Color(50, 100, 5, 180));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(cx - S*0.34, cy - S*0.34, S*0.68, S*0.68));

        // ---- compound eyes ----
        drawEye(g, cx - S*0.195, cy - S*0.055, S*0.185);   // left eye
        drawEye(g, cx + S*0.195, cy - S*0.055, S*0.185);   // right eye

        // ---- between-eye bridge / frons ----
        g.setPaint(headGrad);
        g.fill(new Ellipse2D.Double(cx - S*0.07, cy - S*0.26, S*0.14, S*0.20));

        // ---- head highlight ----
        drawRadial(g, cx - S*0.07, cy - S*0.16, 0, S*0.18,
                new Color(255, 255, 220, 90), new Color(255, 255, 255, 0));

        // ---- proboscis (mouth) ----
        g.setColor(new Color(80, 130, 10));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy + S*0.26, cx, cy + S*0.40));
        // tip bulge
        g.setColor(new Color(60, 110, 8));
        fillCircle(g, cx, cy + S*0.41, S*0.028);

        // ---- small facial bristles ----
        g.setColor(new Color(50, 90, 5, 180));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double[][] bristles = {
            {cx-S*.18, cy+S*.18, cx-S*.26, cy+S*.28},
            {cx-S*.10, cy+S*.22, cx-S*.12, cy+S*.34},
            {cx+S*.18, cy+S*.18, cx+S*.26, cy+S*.28},
            {cx+S*.10, cy+S*.22, cx+S*.12, cy+S*.34},
        };
        for (double[] b : bristles)
            g.draw(new Line2D.Double(b[0], b[1], b[2], b[3]));

        g.dispose();

        String outPath = "src/main/resources/icon.png";
        ImageIO.write(img, "PNG", new File(outPath));
        System.out.println("Written: " + outPath);
    }

    private static void drawEye(Graphics2D g, double ex, double ey, double r) {
        // Outer eye: deep red faceted compound eye
        RadialGradientPaint eyePaint = new RadialGradientPaint(
                new Point2D.Double(ex - r*0.2, ey - r*0.2),
                (float) r,
                new float[]{0f, 0.55f, 1f},
                new Color[]{
                    new Color(255, 60, 40),
                    new Color(180, 10, 10),
                    new Color(60, 0, 0)
                });
        g.setPaint(eyePaint);
        g.fill(new Ellipse2D.Double(ex - r, ey - r, r*2, r*2));

        // Facet hex lines
        g.setColor(new Color(255, 100, 80, 80));
        g.setStroke(new BasicStroke(1.2f));
        for (int fi = 0; fi < 8; fi++) {
            double angle = fi * Math.PI / 4;
            g.draw(new Line2D.Double(ex, ey,
                ex + Math.cos(angle) * r * 0.88,
                ey + Math.sin(angle) * r * 0.88));
        }
        // Concentric ring
        g.setColor(new Color(255, 80, 60, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(ex - r*0.55, ey - r*0.55, r*1.1, r*1.1));

        // Eye outline
        g.setColor(new Color(30, 0, 0, 200));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(ex - r, ey - r, r*2, r*2));

        // Specular highlight
        g.setColor(new Color(255, 200, 200, 170));
        g.fill(new Ellipse2D.Double(ex - r*0.55, ey - r*0.62, r*0.38, r*0.28));
    }

    private static void drawRadial(Graphics2D g,
                                    double cx, double cy,
                                    double innerR, double outerR,
                                    Color inner, Color outer) {
        RadialGradientPaint p = new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) outerR,
                new float[]{(float)(innerR / outerR), 1f},
                new Color[]{inner, outer});
        g.setPaint(p);
        g.fill(new Ellipse2D.Double(cx - outerR, cy - outerR, outerR*2, outerR*2));
    }

    private static void fillCircle(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r*2, r*2));
    }

    private static void fillPoly(Graphics2D g, double[] xs, double[] ys) {
        int n = xs.length;
        int[] ix = new int[n], iy = new int[n];
        for (int i = 0; i < n; i++) { ix[i] = (int) xs[i]; iy[i] = (int) ys[i]; }
        g.fillPolygon(ix, iy, n);
    }

    private static void drawPoly(Graphics2D g, double[] xs, double[] ys) {
        int n = xs.length;
        int[] ix = new int[n], iy = new int[n];
        for (int i = 0; i < n; i++) { ix[i] = (int) xs[i]; iy[i] = (int) ys[i]; }
        g.drawPolygon(ix, iy, n);
    }
}
