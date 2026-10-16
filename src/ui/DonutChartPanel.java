package ui;

import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/** Ring chart with the total in the middle and a legend (colour dot, label, percent) underneath. */
public class DonutChartPanel extends JPanel {

    public static final class Slice {
        final String label; final double value; final Color color;
        public Slice(String label, double value, Color color) { this.label = label; this.value = value; this.color = color; }
    }

    private final List<Slice> slices = new ArrayList<>();
    private String centerCaption = "Total";

    public DonutChartPanel() { setOpaque(false); setPreferredSize(new Dimension(240, 280)); }

    public void setData(List<Slice> list, String caption) {
        slices.clear();
        slices.addAll(list);
        centerCaption = caption;
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        double total = 0;
        for (Slice s : slices) total += s.value;
        int legendH = slices.size() * 24 + 6;
        int d = Math.max(60, Math.min(w - 20, h - legendH - 14));
        int x = (w - d) / 2, y = 4;
        float thick = Math.max(18f, d * 0.17f);
        Stroke ring = new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        Arc2D.Double arc = new Arc2D.Double(x + thick / 2, y + thick / 2, d - thick, d - thick, 90, -360, Arc2D.OPEN);
        g2.setStroke(ring);
        g2.setColor(new Color(236, 241, 247));
        g2.draw(arc);
        if (total > 0) {
            double start = 90;
            for (Slice s : slices) {
                if (s.value <= 0) continue;
                double extent = -360.0 * s.value / total;
                double gap = slices.stream().filter(q -> q.value > 0).count() > 1 ? 2.2 : 0;
                g2.setColor(s.color);
                g2.setStroke(ring);
                g2.draw(new Arc2D.Double(x + thick / 2, y + thick / 2, d - thick, d - thick, start - gap / 2, extent + gap, Arc2D.OPEN));
                start += extent;
            }
        }
        String big = String.valueOf((long) total);
        g2.setFont(UITheme.font(Font.BOLD, Math.max(20, d / 5)));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.drawString(big, x + (d - fm.stringWidth(big)) / 2, y + d / 2 + fm.getAscent() / 3);
        g2.setFont(UITheme.font(Font.PLAIN, 12));
        fm = g2.getFontMetrics();
        g2.setColor(UITheme.TEXT_MUTED);
        g2.drawString(centerCaption, x + (d - fm.stringWidth(centerCaption)) / 2, y + d / 2 + 22);

        int ly = y + d + 22;
        g2.setFont(UITheme.font(Font.PLAIN, 13));
        fm = g2.getFontMetrics();
        for (Slice s : slices) {
            g2.setColor(s.color);
            g2.fillRoundRect(14, ly - 9, 9, 9, 3, 3);
            g2.setColor(UITheme.TEXT_LIGHT);
            g2.drawString(s.label, 31, ly);
            String pct = total > 0 ? Math.round(100.0 * s.value / total) + "%" : "0%";
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString(s.value > 0 || total == 0 ? pct + "  \u00B7  " + (long) s.value : pct, w - 14 - fm.stringWidth(s.value > 0 || total == 0 ? pct + "  \u00B7  " + (long) s.value : pct), ly);
            ly += 24;
        }
        g2.dispose();
    }
}
