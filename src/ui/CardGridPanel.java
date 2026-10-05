package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Scrollable panel that lays vehicle cards out in as many columns as fit (a "responsive" grid):
 * widen the window and more cards appear per row; narrow it and they wrap.
 */
public class CardGridPanel extends JPanel implements Scrollable {

    private static final int MIN_CARD_W = 260, MAX_CARD_W = 340, GAP = 20;

    public CardGridPanel() {
        setOpaque(false);
        setLayout(new GridLayoutManager());
        setBorder(BorderFactory.createEmptyBorder(8, 24, 24, 24));
    }

    private static final class GridLayoutManager implements LayoutManager {
        @Override public void addLayoutComponent(String n, Component c) { }
        @Override public void removeLayoutComponent(Component c) { }
        @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(MIN_CARD_W, 100); }

        private int columns(int avail) { return Math.max(1, (avail + GAP) / (MIN_CARD_W + GAP)); }
        private int cardWidth(int avail, int cols) {
            return Math.min(MAX_CARD_W, (avail - (cols - 1) * GAP) / cols);
        }

        @Override public Dimension preferredLayoutSize(Container p) {
            Insets in = p.getInsets();
            int width = p.getWidth() > 0 ? p.getWidth() : 1000;
            return new Dimension(width, measure(p, width - in.left - in.right, false) + in.top + in.bottom);
        }

        @Override public void layoutContainer(Container p) {
            Insets in = p.getInsets();
            measure(p, p.getWidth() - in.left - in.right, true);
        }

        /** Computes (and when place = true, applies) the layout; returns the content height. */
        private int measure(Container p, int avail, boolean place) {
            Insets in = p.getInsets();
            int cols = columns(avail), cw = cardWidth(avail, cols);
            int rowH = 0, y = in.top, col = 0, count = 0;
            for (Component c : p.getComponents()) {
                if (!(c instanceof VehicleCardPanel)) {          // e.g. the "no vehicles" message
                    int h = c.getPreferredSize().height;
                    if (place) c.setBounds(in.left, y, avail, h);
                    y += h + GAP; continue;
                }
                ((VehicleCardPanel) c).setCardWidth(cw);
                rowH = Math.max(rowH, c.getPreferredSize().height);
                count++;
            }
            if (count == 0) return y - in.top;
            int x0 = in.left;
            for (Component c : p.getComponents()) {
                if (!(c instanceof VehicleCardPanel)) continue;
                if (place) c.setBounds(x0 + col * (cw + GAP), y, cw, rowH);
                if (++col == cols) { col = 0; y += rowH + GAP; }
            }
            if (col != 0) y += rowH + GAP;
            return y - in.top - GAP;
        }
    }

    // ---- Scrollable: follow the viewport width so the columns re-flow when the window is resized
    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(900, 500); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 30; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
