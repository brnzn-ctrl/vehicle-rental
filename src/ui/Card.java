package ui;

import util.UITheme;

import javax.swing.*;
import java.awt.*;

/** White rounded panel with a soft shadow: the building block of the dashboard. */
public class Card extends JPanel {
    private static final int SHADOW = 4, ARC = 18;

    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(SHADOW + 16, SHADOW + 18, SHADOW + 18, SHADOW + 18));
    }

    public Card() { this(new BorderLayout()); }

    /** Card heading: bold title on the left, optional small text/link on the right. */
    public static JComponent header(String title, JComponent right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(UITheme.font(Font.BOLD, 15));
        t.setForeground(UITheme.TEXT_LIGHT);
        p.add(t, BorderLayout.WEST);
        if (right != null) p.add(right, BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return p;
    }

    /** Small rounded tag such as "This Year" next to a card title. */
    public static JLabel chip(String text) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(244, 247, 251));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(UITheme.font(Font.PLAIN, 12));
        l.setForeground(UITheme.TEXT_MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return l;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        for (int i = SHADOW; i >= 1; i--) {
            g2.setColor(new Color(30, 43, 60, 5));
            g2.fillRoundRect(SHADOW - i, SHADOW - i + 2, w - 2 * (SHADOW - i), h - 2 * (SHADOW - i) - 2, ARC + i, ARC + i);
        }
        g2.setColor(UITheme.CARD_BG);
        g2.fillRoundRect(SHADOW, SHADOW, w - 2 * SHADOW, h - 2 * SHADOW - 2, ARC, ARC);
        g2.setColor(new Color(236, 241, 247));
        g2.drawRoundRect(SHADOW, SHADOW, w - 2 * SHADOW - 1, h - 2 * SHADOW - 3, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}
