package util;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Global "modern" look for every Swing screen: flat rounded buttons, soft input borders, thin scroll bars,
 * underline tabs and clean tables. It is installed once (UITheme.applyGlobalFontScale) and only changes how
 * things are drawn; it never touches the data, the listeners or any application logic.
 */
public final class ModernUI {
    private ModernUI() { }

    private static boolean installed;
    private static final String POLISHED = "modernui.polished";

    public static void install() {
        if (installed) return;
        installed = true;

        Font base = UITheme.font(Font.PLAIN, 14);
        Font bold = UITheme.font(Font.BOLD, 14);
        for (String key : new String[] {
            "Label.font", "Button.font", "TextField.font", "PasswordField.font", "FormattedTextField.font",
            "TextArea.font", "ComboBox.font", "CheckBox.font", "RadioButton.font", "Table.font", "List.font",
            "TabbedPane.font", "ToolTip.font", "OptionPane.messageFont", "OptionPane.buttonFont", "TextPane.font",
            "EditorPane.font", "Spinner.font", "MenuItem.font", "Menu.font", "MenuBar.font", "PopupMenu.font",
            "CheckBoxMenuItem.font", "RadioButtonMenuItem.font", "ToggleButton.font", "TitledBorder.font" }) {
            UIManager.put(key, new FontUIResource(base));
        }
        UIManager.put("TableHeader.font", new FontUIResource(UITheme.font(Font.BOLD, 12)));
        UIManager.put("TitledBorder.font", new FontUIResource(bold));
        UIManager.put("Table.rowHeight", 38);

        Color text = UITheme.TEXT_LIGHT, page = UITheme.BG_DARK, white = Color.WHITE;
        for (String key : new String[] { "Panel.background", "OptionPane.background", "RootPane.background",
                "CheckBox.background", "RadioButton.background", "TabbedPane.background", "Viewport.background",
                "ScrollPane.background", "SplitPane.background", "Slider.background", "Spinner.background" }) {
            UIManager.put(key, new ColorUIResource(page));
        }
        for (String key : new String[] { "Label.foreground", "CheckBox.foreground", "RadioButton.foreground",
                "TextField.foreground", "PasswordField.foreground", "TextArea.foreground", "ComboBox.foreground",
                "Table.foreground", "List.foreground", "OptionPane.messageForeground", "TabbedPane.foreground",
                "TitledBorder.titleColor", "Spinner.foreground", "ToolTip.foreground" }) {
            UIManager.put(key, new ColorUIResource(text));
        }
        for (String key : new String[] { "TextField.background", "PasswordField.background", "TextArea.background",
                "ComboBox.background", "Table.background", "List.background", "FormattedTextField.background",
                "TextField.inactiveBackground", "PasswordField.inactiveBackground", "ComboBox.disabledBackground" }) {
            UIManager.put(key, new ColorUIResource(white));
        }
        UIManager.put("TextField.inactiveBackground", new ColorUIResource(new Color(238, 242, 247)));
        UIManager.put("ComboBox.disabledBackground", new ColorUIResource(new Color(238, 242, 247)));
        UIManager.put("TextField.caretForeground", new ColorUIResource(UITheme.RED_ACCENT));
        UIManager.put("PasswordField.caretForeground", new ColorUIResource(UITheme.RED_ACCENT));
        UIManager.put("TextField.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("TextField.selectionForeground", new ColorUIResource(text));
        UIManager.put("Table.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("Table.selectionForeground", new ColorUIResource(text));
        UIManager.put("Table.gridColor", new ColorUIResource(new Color(238, 242, 247)));
        UIManager.put("List.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("List.selectionForeground", new ColorUIResource(text));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(text));
        UIManager.put("TableHeader.background", new ColorUIResource(new Color(240, 245, 250)));
        UIManager.put("TableHeader.foreground", new ColorUIResource(UITheme.TEXT_MUTED));
        UIManager.put("TableHeader.cellBorder", new BorderUIResource(new EmptyBorder(10, 12, 10, 12)));
        UIManager.put("ToolTip.background", new ColorUIResource(UITheme.NAVY));
        UIManager.put("ToolTip.foreground", new ColorUIResource(white));
        UIManager.put("ToolTip.border", new BorderUIResource(new EmptyBorder(5, 9, 5, 9)));
        UIManager.put("MenuBar.background", new ColorUIResource(white));
        UIManager.put("Menu.background", new ColorUIResource(white));
        UIManager.put("MenuItem.background", new ColorUIResource(white));
        UIManager.put("PopupMenu.background", new ColorUIResource(white));
        UIManager.put("MenuBar.border", new BorderUIResource(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER)));
        UIManager.put("Menu.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(UITheme.TINT));
        UIManager.put("Menu.selectionForeground", new ColorUIResource(text));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(text));
        UIManager.put("Menu.foreground", new ColorUIResource(text));
        UIManager.put("MenuItem.foreground", new ColorUIResource(text));
        UIManager.put("OptionPane.border", new BorderUIResource(new EmptyBorder(16, 18, 14, 18)));

        Border field = new BorderUIResource(new CompoundBorder(new RoundBorder(UITheme.BORDER, 10), new EmptyBorder(6, 10, 6, 10)));
        UIManager.put("TextField.border", field);
        UIManager.put("PasswordField.border", field);
        UIManager.put("FormattedTextField.border", field);
        UIManager.put("Spinner.border", new BorderUIResource(new RoundBorder(UITheme.BORDER, 10)));
        UIManager.put("ScrollPane.border", new BorderUIResource(new RoundBorder(UITheme.BORDER, 12)));
        UIManager.put("TextArea.border", new BorderUIResource(new EmptyBorder(6, 8, 6, 8)));

        UIManager.put("ButtonUI", ButtonUI.class.getName());
        UIManager.put("ToggleButtonUI", ButtonUI.class.getName());
        UIManager.put("ScrollBarUI", ScrollUI.class.getName());
        UIManager.put("TabbedPaneUI", TabsUI.class.getName());
        UIManager.put("ComboBoxUI", ComboUI.class.getName());
        UIManager.put("ToolTipManager.enableToolTipMode", "allWindows");

        // Tables are polished the moment they become visible (renderers can't be set from UIManager).
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEventListener) ev -> {
            if (!(ev instanceof HierarchyEvent)) return;
            HierarchyEvent he = (HierarchyEvent) ev;
            if ((he.getChangeFlags() & (HierarchyEvent.DISPLAYABILITY_CHANGED | HierarchyEvent.SHOWING_CHANGED)) == 0) return;
            Component c = he.getComponent();
            if (c instanceof JTable) polishTable((JTable) c);
        }, AWTEvent.HIERARCHY_EVENT_MASK);
    }

    // ------------------------------------------------------------------ tables

    public static void polishTable(JTable t) {
        if (Boolean.TRUE.equals(t.getClientProperty(POLISHED))) return;
        t.putClientProperty(POLISHED, Boolean.TRUE);
        t.setRowHeight(Math.max(t.getRowHeight(), 38));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(UITheme.TINT);
        t.setSelectionForeground(UITheme.TEXT_LIGHT);
        t.setGridColor(new Color(238, 242, 247));
        CellRenderer r = new CellRenderer();
        t.setDefaultRenderer(Object.class, r);
        t.setDefaultRenderer(Number.class, r);
        t.setDefaultRenderer(String.class, r);
        t.setDefaultRenderer(java.util.Date.class, r);
        t.setDefaultRenderer(java.time.LocalDate.class, r);
        JTableHeader h = t.getTableHeader();
        if (h != null) {
            h.setDefaultRenderer(new HeaderRenderer());
            h.setReorderingAllowed(false);
            h.setPreferredSize(new Dimension(h.getPreferredSize().width, 40));
            h.setBackground(new Color(240, 245, 250));
        }
        Container p = t.getParent();
        if (p instanceof JViewport) {
            ((JViewport) p).setBackground(Color.WHITE);
        }
    }

    /** Body cells: padded text, and "Status" columns are drawn as coloured pills. */
    private static final class CellRenderer extends DefaultTableCellRenderer {
        private boolean pill;
        private Color pillBg = UITheme.GRAY_SOFT, pillFg = UITheme.TEXT_LIGHT;

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, false, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(UITheme.font(Font.PLAIN, 13));
            setForeground(UITheme.TEXT_LIGHT);
            setBackground(sel ? UITheme.TINT : Color.WHITE);
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            String name = table.getColumnName(col);
            pill = value != null && name != null && name.trim().equalsIgnoreCase("status") && !String.valueOf(value).isEmpty();
            if (pill) {
                Color[] c = statusColors(String.valueOf(value));
                pillBg = c[0]; pillFg = c[1];
                setForeground(pillFg);
                setFont(UITheme.font(Font.BOLD, 12));
                setText("");   // the pill paints its own text
            }
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            if (!pill) { super.paintComponent(g); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            String s = statusText;
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(s) + 24, h = 24;
            int x = 12, y = (getHeight() - h) / 2;
            g2.setColor(pillBg);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(pillFg);
            g2.drawString(s, x + 12, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }

        private String statusText = "";
        @Override public void setValue(Object value) {
            super.setValue(value);
            statusText = value == null ? "" : String.valueOf(value);
            statusText = statusText.isEmpty() ? "" : Character.toUpperCase(statusText.charAt(0)) + statusText.substring(1);
        }
    }

    /** Soft background + readable text colour for a status word (shared with the dashboard). */
    public static Color[] statusColors(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase();
        Color green = new Color(31, 168, 107), red = new Color(229, 57, 77), amber = new Color(217, 130, 12),
              blue = new Color(45, 108, 223), gray = new Color(100, 116, 139);
        Color fg;
        if (s.matches(".*(available|approved|returned|paid|completed|done|active|ok|in stock|enough).*") && !s.contains("inactive") && !s.contains("unavailable")) fg = green;
        else if (s.matches(".*(ongoing|rented|renting|released|received|on trip).*")) fg = blue;
        else if (s.matches(".*(pending|low|reserved|partial|unpaid|due).*")) fg = amber;
        else if (s.matches(".*(cancel|reject|critical|overdue|damag|late|unavailable|maintenance).*")) fg = red;
        else fg = gray;
        Color bg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30);
        // blend onto white so the pill colour is opaque (table rows repaint with different backgrounds)
        bg = new Color((int) (255 - (255 - fg.getRed()) * 0.13), (int) (255 - (255 - fg.getGreen()) * 0.13), (int) (255 - (255 - fg.getBlue()) * 0.13));
        return new Color[] { bg, fg };
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, false, false, row, col);
            setBackground(new Color(240, 245, 250));
            setForeground(UITheme.TEXT_MUTED);
            setFont(UITheme.font(Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(true);
            return this;
        }
    }

    // ------------------------------------------------------------------ borders

    public static final class RoundBorder extends AbstractBorder {
        private final Color color; private final int arc;
        public RoundBorder(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // hide the square corners of the component's own fill, then draw the soft outline
            Color bg = c.getParent() != null ? c.getParent().getBackground() : UITheme.BG_DARK;
            java.awt.geom.Area corners = new java.awt.geom.Area(new Rectangle(x, y, w, h));
            corners.subtract(new java.awt.geom.Area(new RoundRectangle2D.Float(x, y, w - 1, h - 1, arc, arc)));
            g2.setColor(bg);
            g2.fill(corners);
            boolean focus = c instanceof JComponent && c.hasFocus();
            g2.setColor(focus ? UITheme.RED_ACCENT : color);
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1.5f, h - 1.5f, arc, arc));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
    }

    // ------------------------------------------------------------------ buttons

    /** Flat rounded button. Red filled when the code gave it the accent colour (primary), white with an outline otherwise. */
    public static class ButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent c) { return new ButtonUI(); }

        @Override public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (b.getBorder() == null || b.getBorder() instanceof javax.swing.plaf.UIResource) {
                b.setBorder(new BorderUIResource(new EmptyBorder(8, 16, 8, 16)));
            }
        }

        static boolean primary(AbstractButton b) {
            Color bg = b.getBackground();
            return bg != null && (bg.equals(UITheme.RED_ACCENT) || Boolean.TRUE.equals(b.getClientProperty("ui.primary")));
        }

        @Override public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            boolean pressed = b.getModel().isArmed() && b.getModel().isPressed();
            boolean hover = b.getModel().isRollover() && b.isEnabled();
            boolean prim = primary(b);
            boolean flat = Boolean.TRUE.equals(b.getClientProperty("ui.flat"));
            if (!flat) {
                Color fill;
                if (!b.isEnabled()) fill = prim ? new Color(255, 170, 180) : new Color(244, 246, 250);
                else if (prim) fill = pressed ? new Color(200, 20, 45) : hover ? UITheme.RED_ACCENT_HOVER : UITheme.RED_ACCENT;
                else fill = pressed ? UITheme.TINT : hover ? new Color(246, 249, 253) : Color.WHITE;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
                if (!prim) {
                    g2.setColor(hover ? new Color(205, 214, 228) : UITheme.BORDER);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                }
            }
            g2.dispose();
            Color fg = !b.isEnabled() ? new Color(160, 170, 186)
                : prim && !flat ? Color.WHITE : (b.getForeground() == null ? UITheme.TEXT_LIGHT : b.getForeground());
            if (!prim && b.getForeground() != null && (b.getForeground().equals(Color.BLACK) || b.getForeground() instanceof javax.swing.plaf.UIResource)) fg = b.isEnabled() ? UITheme.TEXT_LIGHT : fg;
            Color old = b.getForeground();
            b.setForeground(fg);
            super.paint(g, c);
            b.setForeground(old);
        }

        @Override protected void paintText(Graphics g, JComponent c, Rectangle r, String text) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font f = b.getFont();
            if (!f.isBold() && (primary(b))) f = f.deriveFont(Font.BOLD);
            g2.setFont(f);
            g2.setColor(b.getForeground());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, r.x, r.y + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ scroll bars

    public static class ScrollUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) { return new ScrollUI(); }
        @Override protected void configureScrollBarColors() { trackColor = new Color(0, 0, 0, 0); thumbColor = new Color(203, 213, 225); }
        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(11, 11); }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private static JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() || isDragging ? new Color(160, 174, 192) : thumbColor);
            boolean vert = scrollbar.getOrientation() == Adjustable.VERTICAL;
            if (vert) g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 5, r.height - 4, 8, 8);
            else g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 5, 8, 8);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ tabs (underline style)

    public static class TabsUI extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) { return new TabsUI(); }
        @Override protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(9, 18, 9, 18);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(0, 14, 0, 0);
        }
        @Override protected void paintTabBackground(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) { }
        @Override protected void paintContentBorder(Graphics g, int p, int sel) { }
        @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] r, int i, Rectangle ir, Rectangle tr, boolean sel) { }
        @Override protected int calculateTabHeight(int p, int i, int fh) { return fh + 20; }
        @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UITheme.BORDER);
            g2.fillRect(x, y + h - 1, w, 1);
            if (sel) { g2.setColor(UITheme.RED_ACCENT); g2.fillRoundRect(x + 10, y + h - 3, w - 20, 3, 3, 3); }
            g2.dispose();
        }
        @Override protected void paintText(Graphics g, int p, Font font, FontMetrics fm, int i, String title, Rectangle tr, boolean sel) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(sel ? font.deriveFont(Font.BOLD) : font);
            g2.setColor(sel ? UITheme.TEXT_LIGHT : UITheme.TEXT_MUTED);
            g2.drawString(title, tr.x, tr.y + fm.getAscent());
            g2.dispose();
        }
        @Override protected void paintTabArea(Graphics g, int p, int sel) {
            g.setColor(UITheme.BORDER);
            g.fillRect(0, calculateTabAreaHeight(p, runCount, maxTabHeight) - 1, tabPane.getWidth(), 1);
            super.paintTabArea(g, p, sel);
        }
    }

    // ------------------------------------------------------------------ combo box

    public static class ComboUI extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) { return new ComboUI(); }
        @Override protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UITheme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            b.setBorder(new EmptyBorder(0, 0, 0, 0));
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(28, 20));
            return b;
        }
        @Override public void installUI(JComponent c) {
            super.installUI(c);
            JComboBox<?> box = (JComboBox<?>) c;
            box.setBackground(Color.WHITE);
            box.setBorder(new BorderUIResource(new CompoundBorder(new RoundBorder(UITheme.BORDER, 10), new EmptyBorder(2, 6, 2, 0))));
        }
        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(Color.WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
}
