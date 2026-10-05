package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Look of the customer portal only (light warm background, white cards, gold accent, script headings).
 * The staff screens keep the blue UITheme; nothing here changes them.
 */
public final class CustomerTheme {
    private CustomerTheme() { }

    public static final Color BG_TOP     = new Color(252, 250, 246);
    public static final Color BG_BOTTOM  = new Color(236, 239, 246);
    public static final Color CARD_FILL  = new Color(255, 255, 255, 240);
    public static final Color CARD_EDGE  = new Color(0, 0, 0, 30);
    public static final Color GOLD       = new Color(184, 140, 20);
    public static final Color GOLD_HOVER = new Color(205, 158, 28);
    public static final Color TEXT       = new Color(34, 36, 48);
    public static final Color MUTED      = new Color(105, 110, 126);
    public static final Color GREEN      = new Color(30, 150, 80);
    public static final Color RED        = new Color(204, 52, 52);
    public static final Color FIELD_BG   = new Color(255, 255, 255, 235);

    private static String scriptFamily;

    /** Elegant script font like the reference (Segoe Script on Windows); falls back to an italic serif. */
    public static Font script(float size) {
        if (scriptFamily == null) {
            scriptFamily = "Serif";
            java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String f : new String[]{"Segoe Script", "Lucida Handwriting", "Brush Script MT", "Georgia"}) {
                if (have.contains(f)) { scriptFamily = f; break; }
            }
        }
        return new Font(scriptFamily, "Serif".equals(scriptFamily) || "Georgia".equals(scriptFamily) ? Font.ITALIC : Font.PLAIN, Math.round(size));
    }

    public static Font body(float size)  { return new Font("Segoe UI", Font.PLAIN, Math.round(size)); }
    public static Font bold(float size)  { return new Font("Segoe UI", Font.BOLD, Math.round(size)); }
    public static Font italic(float size){ return new Font("Segoe UI", Font.ITALIC, Math.round(size)); }

    /** Smooth edges and text for everything we paint ourselves. */
    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    /** Pill button painted by us, so it looks the same on every Java look-and-feel. gold = filled, otherwise outlined. */
    public static JButton pill(String text, boolean gold) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                boolean hover = getModel().isRollover() || getModel().isArmed();
                int arc = getHeight();
                if (gold) {
                    g2.setColor(hover ? GOLD_HOVER : GOLD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                    setForeground(Color.WHITE);
                } else {
                    g2.setColor(hover ? new Color(0, 0, 0, 16) : new Color(0, 0, 0, 7));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.setColor(hover ? GOLD : new Color(0, 0, 0, 70));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    setForeground(TEXT);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFont(bold(15));
        b.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Calls onChange(true/false) when the mouse enters / leaves c or any of its children. */
    public static void trackHover(JComponent c, java.util.function.Consumer<Boolean> onChange) {
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { onChange.accept(true); }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), c);
                if (!c.contains(p)) onChange.accept(false);
            }
        };
        attach(c, m);
    }

    private static void attach(Component comp, MouseAdapter m) {
        comp.addMouseListener(m);
        if (comp instanceof Container) for (Component k : ((Container) comp).getComponents()) attach(k, m);
    }
}
