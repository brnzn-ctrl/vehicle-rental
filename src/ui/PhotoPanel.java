package ui;

import util.CustomerTheme;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** A photo with rounded corners that always fills its box ("cover" fit). Draws a car icon when there is no photo. */
public class PhotoPanel extends JComponent {

    private final BufferedImage photo;      // may be null
    private final int arc;
    private BufferedImage cache;            // photo scaled for the current size
    private int cacheW, cacheH;

    public PhotoPanel(BufferedImage photo, int arc) {
        this.photo = photo;
        this.arc = arc;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g2 = CustomerTheme.smooth(g);
        Shape clip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
        g2.setClip(clip);
        if (photo != null) {
            if (cache == null || cacheW != w || cacheH != h) {
                cache = scaleCover(photo, w, h);
                cacheW = w; cacheH = h;
            }
            g2.drawImage(cache, 0, 0, null);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(226, 230, 240), 0, h, new Color(206, 212, 226)));
            g2.fillRect(0, 0, w, h);
            drawCarIcon(g2, w, h);
        }
        // soft fade at the bottom edge of the photo, like a glossy card
        g2.setPaint(new GradientPaint(0, h * 0.65f, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 35)));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    /** Scales in steps so large photos stay sharp when they are shrunk. */
    private static BufferedImage scaleCover(BufferedImage src, int w, int h) {
        double sr = (double) src.getWidth() / src.getHeight(), tr = (double) w / h;
        int cw, ch, x, y;
        if (sr > tr) { ch = src.getHeight(); cw = (int) Math.round(ch * tr); x = (src.getWidth() - cw) / 2; y = 0; }
        else         { cw = src.getWidth();  ch = (int) Math.round(cw / tr); x = 0; y = (src.getHeight() - ch) / 2; }
        BufferedImage cur = src.getSubimage(x, y, Math.max(1, cw), Math.max(1, ch));
        while (cur.getWidth() / 2 >= w && cur.getHeight() / 2 >= h) cur = step(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        return step(cur, w, h);
    }

    private static BufferedImage step(BufferedImage s, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(s, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static void drawCarIcon(Graphics2D g0, int w, int h) {
        Graphics2D g2 = (Graphics2D) g0.create();   // own copy, so the caller's transform is left alone
        float s = Math.min(w, h * 1.6f) / 260f;
        float cx = w / 2f, cy = h / 2f + 6 * s;
        g2.translate(cx, cy);
        g2.scale(s, s);
        g2.setColor(new Color(255, 255, 255, 180));
        Path2D body = new Path2D.Float();
        body.moveTo(-95, 18); body.lineTo(-88, -4); body.lineTo(-52, -10); body.lineTo(-30, -36);
        body.lineTo(34, -36); body.lineTo(60, -10); body.lineTo(92, -4); body.lineTo(98, 18);
        body.lineTo(98, 30); body.lineTo(-95, 30); body.closePath();
        g2.fill(body);
        g2.setColor(new Color(150, 156, 172));
        g2.fillOval(-72, 14, 34, 34); g2.fillOval(44, 14, 34, 34);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(-62, 24, 14, 14); g2.fillOval(54, 24, 14, 14);
        g2.dispose();
    }
}
