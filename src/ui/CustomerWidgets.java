package ui;

import util.CustomerTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/** Small custom-painted controls for the customer portal (they look the same on every Java look-and-feel). */
final class CustomerWidgets {
    private CustomerWidgets() { }

    /** Light gradient page background with a soft gold glow in the corner. */
    static final class Backdrop extends JPanel {
        Backdrop() { setLayout(new BorderLayout()); setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = CustomerTheme.smooth(g);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, CustomerTheme.BG_TOP, 0, h, CustomerTheme.BG_BOTTOM));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new RadialGradientPaint(new Point(w - 80, -40), Math.max(w, h) * 0.7f,
                new float[]{0f, 1f}, new Color[]{new Color(212, 175, 55, 38), new Color(212, 175, 55, 0)}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /** Rounded search box with a grey hint text while empty. */
    static final class SearchField extends JTextField {
        private final String hint;
        SearchField(String hint, int columns) {
            super(columns);
            this.hint = hint;
            setOpaque(false);
            setFont(CustomerTheme.body(16));
            setForeground(CustomerTheme.TEXT);
            setCaretColor(CustomerTheme.GOLD);
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = CustomerTheme.smooth(g);
            g2.setColor(CustomerTheme.FIELD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(hasFocus() ? CustomerTheme.GOLD : new Color(0, 0, 0, 60));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            if (getText().isEmpty()) {
                g2.setColor(CustomerTheme.MUTED);
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(hint, in.left, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Category filter chip: gold when selected. */
    static final class Chip extends JToggleButton {
        Chip(String text) {
            super(text);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setFont(CustomerTheme.bold(14));
            setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = CustomerTheme.smooth(g);
            int a = getHeight();
            if (isSelected()) {
                g2.setColor(CustomerTheme.GOLD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, a, a);
                setForeground(Color.WHITE);
            } else {
                g2.setColor(getModel().isRollover() ? new Color(0, 0, 0, 16) : new Color(255, 255, 255, 200));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, a, a);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, a, a);
                setForeground(CustomerTheme.TEXT);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Top navigation tab with a gold underline when selected. */
    static final class NavTab extends JToggleButton {
        NavTab(String text) {
            super(text);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setFont(CustomerTheme.bold(16));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        @Override protected void paintComponent(Graphics g) {
            setForeground(isSelected() ? CustomerTheme.GOLD : (getModel().isRollover() ? CustomerTheme.TEXT : CustomerTheme.MUTED));
            super.paintComponent(g);
            if (isSelected()) {
                Graphics2D g2 = CustomerTheme.smooth(g);
                g2.setColor(CustomerTheme.GOLD);
                g2.fillRoundRect(14, getHeight() - 4, getWidth() - 28, 3, 3, 3);
                g2.dispose();
            }
        }
    }

    /** Makes a JTable match the light theme (header, striped rows, gold selection, coloured Status column). */
    static void styleTable(JTable t, String statusColumnName) {
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setForeground(CustomerTheme.TEXT);
        t.setSelectionBackground(new Color(250, 236, 190));
        t.setSelectionForeground(CustomerTheme.TEXT);
        t.setFont(CustomerTheme.body(15));

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(246, 247, 251));
                setForeground(CustomerTheme.TEXT);
                int model = tb.convertColumnIndexToModel(c);
                if (statusColumnName != null && statusColumnName.equals(tb.getModel().getColumnName(model)) && v != null) {
                    String s = v.toString();
                    Color col = status(s);
                    setText("\u25CF  " + s.substring(0, 1).toUpperCase() + s.substring(1));
                    setForeground(col);
                    setFont(CustomerTheme.bold(14));
                } else {
                    setFont(CustomerTheme.body(15));
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, cell);
        t.setDefaultRenderer(Integer.class, cell);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(10, 42));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setOpaque(true);
                setBackground(new Color(250, 244, 226));
                setForeground(CustomerTheme.GOLD);
                setFont(CustomerTheme.bold(14));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                return this;
            }
        });
    }

    static Color status(String s) {
        switch (s.toLowerCase()) {
            case "approved":  return CustomerTheme.GREEN;
            case "pending":   return new Color(214, 130, 10);
            case "rejected":  return CustomerTheme.RED;
            case "converted": return new Color(40, 110, 200);
            default:          return CustomerTheme.MUTED;   // cancelled
        }
    }

    /** Light scroll pane with a thin rounded-look border for tables. */
    static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 40)));
        sp.getViewport().setBackground(Color.WHITE);
        sp.setOpaque(false);
        return sp;
    }
}
