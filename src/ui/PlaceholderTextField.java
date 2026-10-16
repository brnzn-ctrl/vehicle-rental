package ui;

import javax.swing.*;
import java.awt.*;

/** Text field that paints a hint while empty and unfocused. */
final class PlaceholderTextField extends JTextField {
    private final String placeholder;

    PlaceholderTextField(int columns, String placeholder) {
        super(columns);
        this.placeholder = placeholder;
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!getText().isEmpty() || hasFocus()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        Color hintColor = UIManager.getColor("TextField.inactiveForeground");
        g2.setColor(hintColor == null ? Color.GRAY : hintColor);
        Insets insets = getInsets();
        FontMetrics metrics = g2.getFontMetrics();
        int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(placeholder, insets.left + 2, y);
        g2.dispose();
    }
}
