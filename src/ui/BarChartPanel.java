package ui;

import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Bar chart drawn from a Map<label, value> (same setData(...) call as before, so Reports keeps working).
 * New look: rounded navy bars, the biggest one in red, dashed grid, value labels and a hover highlight.
 */
public class BarChartPanel extends JPanel {

    private Map<String, BigDecimal> data = Map.of();
    private String title = "";
    private int hover = -1;
    private int[] barX = new int[0];
    private int barW;

    public BarChartPanel() {
        setOpaque(false);
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int old = hover; hover = -1;
                for (int i = 0; i < barX.length; i++) if (e.getX() >= barX[i] - 6 && e.getX() <= barX[i] + barW + 6) hover = i;
                if (old != hover) repaint();
            }
        });
        addMouseListener(new MouseAdapter() { @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); } });
    }

    public void setData(Map<String, BigDecimal> data, String title) {
        this.data = data;
        this.title = title;
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = getWidth(), height = getHeight();
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, width - 1, height - 1, 18, 18);
        g2.setColor(UITheme.BORDER);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 18, 18);

        g2.setFont(UITheme.font(Font.BOLD, 15));
        g2.setColor(UITheme.TEXT_LIGHT);
        g2.drawString(title, 24, 34);

        int left = 64, right = 24, top = 62, bottom = 40;
        int chartBottom = height - bottom, chartHeight = chartBottom - top;
        if (data.isEmpty()) {
            g2.setFont(UITheme.font(Font.PLAIN, 13));
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("No data yet. Click \"Generate Chart\" once there are payments.", left, top + chartHeight / 2);
            g2.dispose();
            return;
        }

        double max = data.values().stream().mapToDouble(BigDecimal::doubleValue).max().orElse(1);
        if (max <= 0) max = 1;
        double mag = Math.pow(10, Math.floor(Math.log10(max)));
        double n = max / mag;
        double axisMax = (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * mag;

        g2.setFont(UITheme.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int y = top + (int) Math.round(chartHeight * (1 - i / 4.0));
            g2.setColor(new Color(232, 237, 244));
            g2.setStroke(i == 0 ? new BasicStroke(1f) : new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 4f}, 0f));
            g2.drawLine(left, y, width - right, y);
            g2.setColor(UITheme.TEXT_MUTED);
            String s = String.format("%,.0f", axisMax * i / 4.0);
            g2.drawString(s, left - 10 - fm.stringWidth(s), y + 4);
        }
        g2.setStroke(new BasicStroke(1f));

        List<String> labels = data.keySet().stream().toList();
        int count = labels.size();
        int area = (width - left - right) / count;
        barW = Math.max(18, Math.min(56, area - 30));
        barX = new int[count];
        String topLabel = null; double best = -1;
        for (String l : labels) if (data.get(l).doubleValue() > best) { best = data.get(l).doubleValue(); topLabel = l; }

        for (int i = 0; i < count; i++) {
            String label = labels.get(i);
            double value = data.get(label).doubleValue();
            int bh = (int) Math.round(value / axisMax * chartHeight);
            int x = left + i * area + (area - barW) / 2;
            barX[i] = x;
            boolean hi = label.equals(topLabel);
            Color c = hi ? UITheme.RED_ACCENT : UITheme.NAVY;
            if (hover >= 0 && hover != i) c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 150);
            g2.setColor(new Color(240, 245, 250));
            g2.fill(new RoundRectangle2D.Float(x, top, barW, chartHeight, 10, 10));
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(x, chartBottom - Math.max(bh, 4), barW, Math.max(bh, 4), 10, 10));

            g2.setFont(UITheme.font(Font.BOLD, 12));
            fm = g2.getFontMetrics();
            String v = "\u20B1" + String.format("%,.0f", value);
            g2.setColor(UITheme.TEXT_LIGHT);
            g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, Math.max(top - 4, chartBottom - bh - 8));

            g2.setFont(UITheme.font(Font.PLAIN, 12));
            fm = g2.getFontMetrics();
            g2.setColor(hover == i ? UITheme.TEXT_LIGHT : UITheme.TEXT_MUTED);
            String lab = label;
            while (fm.stringWidth(lab) > area - 6 && lab.length() > 3) lab = lab.substring(0, lab.length() - 2);
            if (!lab.equals(label)) lab += "\u2026";
            g2.drawString(lab, x + (barW - fm.stringWidth(lab)) / 2, chartBottom + 22);
        }
        g2.dispose();
    }
}
