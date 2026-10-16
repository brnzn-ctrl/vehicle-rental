package util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Look of the staff / admin screens (clean light layout, white cards, navy text, red accent).
 * Only colours, fonts and shapes live here: no application logic.
 * The constant names are unchanged from the old blue theme, so every screen picks the new look up automatically
 * (BG_DARK is now the soft page background, BG_PANEL the white card colour, TEXT_LIGHT the dark main text colour,
 * RED_ACCENT is now really red).
 */
public final class UITheme {
    private UITheme() { }

    // ---- core palette (names kept for compatibility, values are the new design)
    public static final Color BG_DARK      = new Color(243, 246, 250);  // page background
    public static final Color BG_PANEL     = new Color(255, 255, 255);  // cards, sidebar
    public static final Color RED_ACCENT   = new Color(255, 45, 70);    // primary accent (buttons, highlights)
    public static final Color RED_ACCENT_HOVER = new Color(230, 30, 56);
    public static final Color TEXT_LIGHT   = new Color(27, 37, 54);     // main text (dark navy)
    public static final Color TEXT_MUTED   = new Color(122, 133, 153);  // secondary text
    public static final Color CARD_BG      = new Color(255, 255, 255);
    public static final Color GREEN_MONEY  = new Color(31, 168, 107);
    public static final Color GOLD         = RED_ACCENT;                // old name, still works

    // ---- extra design tokens
    public static final Color NAVY         = new Color(30, 43, 60);
    public static final Color TINT         = new Color(231, 240, 249);  // soft blue (selected menu item, icon circles)
    public static final Color BORDER       = new Color(230, 235, 242);
    public static final Color AMBER        = new Color(245, 166, 35);
    public static final Color BLUE         = new Color(59, 130, 246);
    public static final Color GRAY_SOFT    = new Color(226, 232, 240);

    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_TITLE  = new Font(FONT_FAMILY, Font.BOLD, 26);
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_BODY   = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_PRICE  = new Font(FONT_FAMILY, Font.BOLD, 14);

    public static Font font(int style, float size) { return new Font(FONT_FAMILY, style, 1).deriveFont(style, size); }

    /** Installs the global look (fonts, colours, rounded buttons, flat tabs and scroll bars, table styling). */
    public static void applyGlobalFontScale() {
        ModernUI.install();
    }

    /** Applies the page background to any top-level frame's content. */
   public static void styleFrame(JFrame frame) {
    frame.getContentPane().setBackground(BG_DARK);

    try {
        java.net.URL url = UITheme.class.getResource("/resources/RESTRO_LOGO.png");
        if (url != null) {
            java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(url);

            java.util.List<Image> icons = new java.util.ArrayList<>();
            for (int size : new int[]{16, 32, 64, 128}) {
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                        size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(src, 0, 0, size, size, null);
                g.dispose();
                icons.add(img);
            }
            frame.setIconImages(icons);
        }
    } catch (java.io.IOException e) {
        e.printStackTrace();
    }
   }

    /** The red primary button used for main actions (Login, Reserve, Add, etc.). */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(RED_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(font(Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 18, 9, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** A plain white secondary button. */
    public static JButton sidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_PANEL);
        btn.setForeground(TEXT_LIGHT);
        btn.setFont(FONT_BODY);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT_LIGHT);
        return l;
    }

    /** Formats a peso price the same way everywhere: "\u20B11,500.00" */
    public static String peso(java.math.BigDecimal amount) {
        return "\u20B1" + String.format("%,.2f", amount);
    }
    
    
}
