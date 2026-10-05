package util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UITheme {
    private UITheme() { }

    public static final Color BG_DARK      = new Color(11, 61, 145);   // bright royal blue background
    public static final Color BG_PANEL     = new Color(16, 82, 178);   // slightly lighter blue panel background
    public static final Color RED_ACCENT   = new Color(212, 175, 55);  // gold accent (primary buttons, price tags)
    public static final Color RED_ACCENT_HOVER = new Color(230, 195, 90);
    public static final Color TEXT_LIGHT   = new Color(255, 255, 255);
    public static final Color TEXT_MUTED   = new Color(210, 225, 250);
    public static final Color CARD_BG      = new Color(255, 255, 255);
    public static final Color GREEN_MONEY  = new Color(40, 160, 90);
    public static final Color GOLD         = RED_ACCENT;               // clearer name for new code

    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 18);
    public static final Font FONT_PRICE  = new Font("Segoe UI", Font.BOLD, 18);

  
    public static void applyGlobalFontScale() {
        Font base = new Font("Segoe UI", Font.PLAIN, 18);
        Font bold = new Font("Segoe UI", Font.BOLD, 18);
        java.util.List<String> plainKeys = java.util.List.of(
            "Label.font", "Button.font", "TextField.font", "PasswordField.font",
            "TextArea.font", "ComboBox.font", "CheckBox.font", "RadioButton.font",
            "Table.font", "TableHeader.font", "List.font", "TabbedPane.font",
            "ToolTip.font", "OptionPane.messageFont", "OptionPane.buttonFont",
            "TextPane.font", "EditorPane.font", "Spinner.font", "MenuItem.font",
            "Menu.font", "MenuBar.font", "PopupMenu.font", "Panel.font"
        );
        for (String key : plainKeys) UIManager.put(key, base);
        UIManager.put("TitledBorder.font", bold);
        // Table rows need to be taller to fit the bigger font, or text gets clipped.
        UIManager.put("Table.rowHeight", 32);
    }

    /** Applies the dark background + light text to any top-level frame's content. */
    public static void styleFrame(JFrame frame) {
        frame.getContentPane().setBackground(BG_DARK);
    }

    /** A gold "royal" button — used for primary actions (Login, Reserve, Add, etc.). */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(RED_ACCENT);
        btn.setForeground(BG_DARK);
        btn.setFont(FONT_HEADER);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** A plain dark sidebar/secondary button. */
    public static JButton sidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_PANEL);
        btn.setForeground(TEXT_LIGHT);
        btn.setFont(FONT_BODY);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
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

    /** Formats a peso price the same way everywhere: "₱1,500.00" */
    public static String peso(java.math.BigDecimal amount) {
        return "\u20B1" + String.format("%,.2f", amount);
    }
}
