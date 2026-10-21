package ui;

import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Smooth area/line chart with grid, peso axis and a hover tooltip. Pure Java2D, no chart library. */
public class LineChartPanel extends JPanel {

    public static final class Series {
        final String name; final Color color; final double[] values; final boolean area;
        public Series(String name, Color color, double[] values, boolean area) { this.name = name; this.color = color; this.values = values; this.area = area; }
    }

    private String[] labels = new String[0];
    private final List<Series> series = new ArrayList<>();
    private final List<Series> tooltipSeries = new ArrayList<>();
    private int hover = -1;

    public LineChartPanel() {
        setOpaque(false);
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover = indexAt(e.getX()); repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
        });
    }

    public void setData(String[] labels, List<Series> list) {
        setData(labels, list, List.of());
    }

    public void setData(String[] labels, List<Series> list, List<Series> tooltipSeries) {
        this.labels = labels;
        series.clear();
        series.addAll(list);
        this.tooltipSeries.clear();
        this.tooltipSeries.addAll(tooltipSeries);
        repaint();
    }

    private static final int L = 58, R = 14, T = 10, B = 28;

    private int indexAt(int mx) {
        if (labels.length == 0) return -1;
        double step = labels.length > 1 ? (getWidth() - L - R) / (double) (labels.length - 1) : 1;
        int i = (int) Math.round((mx - L) / step);
        return (i < 0 || i >= labels.length) ? -1 : i;
    }

    private double niceMax() {
        double max = 0;
        for (Series s : series) for (double v : s.values) max = Math.max(max, v);
        if (max <= 0) return 1000;
        double mag = Math.pow(10, Math.floor(Math.log10(max)));
        double n = max / mag;
        double nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
        return nice * mag;
    }

    private static String axis(double v) {
        if (v >= 1_000_000) return "\u20B1" + trim(v / 1_000_000) + "M";
        if (v >= 1000) return "\u20B1" + trim(v / 1000) + "K";
        return "\u20B1" + trim(v);
    }
    private static String trim(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int cw = w - L - R, ch = h - T - B;
        double max = niceMax();
        boolean empty = series.stream().allMatch(s -> java.util.Arrays.stream(s.values).allMatch(v -> v == 0));

        g2.setFont(UITheme.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int y = T + (int) Math.round(ch * (1 - i / 4.0));
            g2.setColor(new Color(232, 237, 244));
            g2.setStroke(i == 0 ? new BasicStroke(1f) : new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 4f}, 0f));
            g2.drawLine(L, y, w - R, y);
            g2.setColor(UITheme.TEXT_MUTED);
            String s = axis(max * i / 4.0);
            g2.drawString(s, L - 10 - fm.stringWidth(s), y + 4);
        }
        g2.setStroke(new BasicStroke(1f));
        int n = labels.length;
        if (n == 0) { g2.dispose(); return; }
        double step = n > 1 ? cw / (double) (n - 1) : 0;
        for (int i = 0; i < n; i++) {
            g2.setColor(i == hover ? UITheme.TEXT_LIGHT : UITheme.TEXT_MUTED);
            int x = L + (int) Math.round(i * step);
            g2.drawString(labels[i], x - fm.stringWidth(labels[i]) / 2, h - 8);
        }

        if (hover >= 0) {                      // vertical guide line under the hovered month
            int x = L + (int) Math.round(hover * step);
            g2.setColor(new Color(255, 45, 70, 60));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f));
            g2.drawLine(x, T, x, T + ch);
            g2.setStroke(new BasicStroke(1f));
        }

        for (Series s : series) {
            int m = Math.min(n, s.values.length);       // a series may stop early (months that have not happened yet)
            if (m == 0) continue;
            Point2D.Double[] pts = new Point2D.Double[m];
            for (int i = 0; i < m; i++) pts[i] = new Point2D.Double(L + i * step, T + ch * (1 - Math.min(1, s.values[i] / max)));
            Path2D line = smooth(pts);
            if (s.area) {
                Path2D fill = new Path2D.Double(line);
                fill.lineTo(pts[m - 1].x, T + ch);
                fill.lineTo(pts[0].x, T + ch);
                fill.closePath();
                g2.setPaint(new GradientPaint(0, T, new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 55),
                                              0, T + ch, new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 0)));
                g2.fill(fill);
            }
            g2.setColor(s.color);
            g2.setStroke(new BasicStroke(s.area ? 2.6f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);
            if (hover >= 0 && hover < m) {
                g2.setColor(Color.WHITE);
                g2.fillOval((int) pts[hover].x - 6, (int) pts[hover].y - 6, 12, 12);
                g2.setColor(s.color);
                g2.setStroke(new BasicStroke(2.4f));
                g2.drawOval((int) pts[hover].x - 5, (int) pts[hover].y - 5, 10, 10);
            }
        }

        if (empty) {
            g2.setFont(UITheme.font(Font.PLAIN, 12));
            g2.setColor(UITheme.TEXT_MUTED);
            String msg = "No payments recorded for this period yet";
            g2.drawString(msg, L + (cw - g2.getFontMetrics().stringWidth(msg)) / 2, T + ch / 2 - 10);
        }
        if (hover >= 0) tooltip(g2, step, max, ch);
        g2.dispose();
    }

    private void tooltip(Graphics2D g2, double step, double max, int ch) {
        List<Series> shown = new ArrayList<>();
        for (Series s : series) if (hover < s.values.length) shown.add(s);
        for (Series s : tooltipSeries) if (hover < s.values.length) shown.add(s);
        if (shown.isEmpty()) return;
        int rows = shown.size();
        Font title = UITheme.font(Font.BOLD, 12), body = UITheme.font(Font.PLAIN, 12);
        g2.setFont(body);
        FontMetrics fb = g2.getFontMetrics();
        int w = 0;
        String[] lines = new String[rows];
        for (int i = 0; i < rows; i++) {
            lines[i] = shown.get(i).name + "  " + UITheme.peso(java.math.BigDecimal.valueOf(shown.get(i).values[hover]));
            w = Math.max(w, fb.stringWidth(lines[i]) + 18);
        }
        w = Math.max(w, 90) + 16;
        int h = 28 + rows * 18;
        int x = L + (int) Math.round(hover * step) - w / 2;
        x = Math.max(4, Math.min(getWidth() - w - 4, x));
        int y = T + 2;
        g2.setColor(new Color(30, 43, 60, 20));
        g2.fill(new RoundRectangle2D.Float(x + 1, y + 3, w, h, 12, 12));
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        g2.setColor(UITheme.BORDER);
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        g2.setFont(title);
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.drawString(labels[hover], x + 10, y + 18);
        g2.setFont(body);
        for (int i = 0; i < rows; i++) {
            g2.setColor(shown.get(i).color);
            g2.fillOval(x + 10, y + 28 + i * 18 + 1, 8, 8);
            g2.setColor(UITheme.TEXT_LIGHT);
            g2.drawString(lines[i], x + 24, y + 36 + i * 18);
        }
    }

    /** Catmull-Rom spline through the points, as cubic Beziers (a soft curve instead of zig-zag lines). */
    private static Path2D smooth(Point2D.Double[] p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0].x, p[0].y);
        for (int i = 0; i < p.length - 1; i++) {
            Point2D.Double p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(p.length - 1, i + 2)];
            double c1x = p1.x + (p2.x - p0.x) / 6.0, c1y = p1.y + (p2.y - p0.y) / 6.0;
            double c2x = p2.x - (p3.x - p1.x) / 6.0, c2y = p2.y - (p3.y - p1.y) / 6.0;
            double top = Math.min(p1.y, p2.y), bottom = Math.max(p1.y, p2.y);   // never overshoot between two points
            c1y = Math.max(top, Math.min(bottom, c1y));
            c2y = Math.max(top, Math.min(bottom, c2y));
            path.curveTo(c1x, c1y, c2x, c2y, p2.x, p2.y);
        }
        return path;
    }
}
