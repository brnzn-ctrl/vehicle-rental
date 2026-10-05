package ui;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Draws a simple bar chart from a Map<label, value>. No JFreeChart/library needed. */
public class BarChartPanel extends JPanel {

    private Map<String, BigDecimal> data = Map.of();
    private String title = "";

    public void setData(Map<String, BigDecimal> data, String title) {
        this.data = data;
        this.title = title;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth(), height = getHeight();
        int margin = 50;
        int chartBottom = height - margin;
        int chartTop = margin;
        int chartHeight = chartBottom - chartTop;

        g2.setColor(Color.DARK_GRAY);
        g2.drawString(title, margin, 20);

        if (data.isEmpty()) {
            g2.drawString("No data yet.", margin, chartTop + 20);
            return;
        }

        double max = data.values().stream().mapToDouble(BigDecimal::doubleValue).max().orElse(1);
        if (max == 0) max = 1;

        List<String> labels = data.keySet().stream().toList();
        int barCount = labels.size();
        int barAreaWidth = (width - margin * 2) / barCount;
        int barWidth = Math.max(20, barAreaWidth - 30);

        g2.setColor(Color.GRAY);
        g2.drawLine(margin, chartBottom, width - margin, chartBottom); // x-axis
        g2.drawLine(margin, chartTop, margin, chartBottom);            // y-axis

        Color[] palette = { new Color(66,133,244), new Color(219,68,55), new Color(244,180,0),
                            new Color(15,157,88), new Color(171,71,188), new Color(255,112,67) };

        int x = margin + 15;
        for (int i = 0; i < barCount; i++) {
            String label = labels.get(i);
            double value = data.get(label).doubleValue();
            int barHeight = (int) ((value / max) * (chartHeight - 20));

            g2.setColor(palette[i % palette.length]);
            g2.fillRect(x, chartBottom - barHeight, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.drawString(String.format("%,.0f", value), x, chartBottom - barHeight - 5);

            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2.drawString(label, x + (barWidth - labelWidth) / 2, chartBottom + 15);

            x += barAreaWidth;
        }
    }
}
