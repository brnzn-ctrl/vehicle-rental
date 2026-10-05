package ui;

import java.awt.*;

/** FlowLayout that wraps onto extra rows when the container is too narrow (used for the category chips). */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container target) { return size(target, true); }

    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension d = size(target, false);
        d.width -= getHgap() + 1;
        return d;
    }

    private Dimension size(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container parent = target;
            while (parent.getSize().width == 0 && parent.getParent() != null) parent = parent.getParent();
            if (targetWidth == 0) targetWidth = parent.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap(), vgap = getVgap();
            Insets in = target.getInsets();
            int maxWidth = targetWidth - (in.left + in.right + hgap * 2);

            Dimension dim = new Dimension(0, 0);
            int rowW = 0, rowH = 0;
            for (Component m : target.getComponents()) {
                if (!m.isVisible()) continue;
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowW + d.width > maxWidth && rowW > 0) {       // start a new row
                    dim.width = Math.max(dim.width, rowW);
                    dim.height += rowH + vgap;
                    rowW = 0; rowH = 0;
                }
                if (rowW != 0) rowW += hgap;
                rowW += d.width;
                rowH = Math.max(rowH, d.height);
            }
            dim.width = Math.max(dim.width, rowW);
            dim.height += rowH;
            dim.width += in.left + in.right + hgap * 2;
            dim.height += in.top + in.bottom + vgap * 2;
            return dim;
        }
    }
}
