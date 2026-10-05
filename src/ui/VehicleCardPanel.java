package ui;

import model.Vehicle;
import util.CustomerTheme;
import util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

/**
 * One glass-style vehicle card (see the reference): rounded photo on top, small italic category line,
 * script-style name, detail lines, then the status line and a gold Reserve button.
 * Click anywhere on the card, or the button, to reserve.
 */
public class VehicleCardPanel extends JPanel {

    private static final String IMAGE_FOLDER = "VehicleImages";
    private static final int PAD = 12;
    private static final double PHOTO_RATIO = 0.64;   // photo height / card width

    private final PhotoPanel photo;
    private boolean hover;

    /** @param dealPercent active discount % today, or null when there is none */
    public VehicleCardPanel(Vehicle v, BigDecimal dealPercent, Runnable onReserve) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        photo = new PhotoPanel(load(v), 14);
        photo.setAlignmentX(LEFT_ALIGNMENT);
        add(photo);
        add(Box.createVerticalStrut(10));

        add(label("Restro Rentals", CustomerTheme.italic(12), CustomerTheme.MUTED));
        add(Box.createVerticalStrut(2));
        JLabel name = label(v.getName(), CustomerTheme.script(19), CustomerTheme.TEXT);
        add(name);
        add(Box.createVerticalStrut(8));

        add(detail("Class:", v.getCategoryName()));
        add(detail("Plate:", v.getPlateNumber()));
        add(detail("Price:", util.UITheme.peso(v.getDailyRate()) + " / day"));
        add(Box.createVerticalStrut(6));

        // status line (like the small "Upgrade / Released" line in the reference)
        String status = dealPercent != null
            ? "<html><span style='color:#B88C14'>&#9733; " + dealPercent.stripTrailingZeros().toPlainString() + "% off today</span></html>"
            : "<html><span style='color:#1E9650'>&#9679;</span> <span style='color:#696E7E'>Available now</span></html>";
        JLabel st = label(status, CustomerTheme.body(12), CustomerTheme.MUTED);
        add(st);
        add(Box.createVerticalStrut(10));

        JButton reserve = CustomerTheme.pill("Reserve", true);
        reserve.setAlignmentX(LEFT_ALIGNMENT);
        reserve.setMaximumSize(new Dimension(Integer.MAX_VALUE, reserve.getPreferredSize().height));
        reserve.addActionListener(e -> onReserve.run());
        add(reserve);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onReserve.run(); }
        });
        CustomerTheme.trackHover(this, on -> { hover = on; repaint(); });
        forwardClicks(this, onReserve);
    }

    /** Labels and the photo have their own mouse listeners (hover), which would swallow clicks: pass them on. */
    private static void forwardClicks(Container parent, Runnable onReserve) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JButton) continue;                  // the Reserve button handles its own click
            c.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onReserve.run(); }
            });
            if (c instanceof Container) forwardClicks((Container) c, onReserve);
        }
    }

    /** The grid calls this with the card width it chose, so the photo keeps its shape. */
    public void setCardWidth(int w) {
        int pw = w - 2 * PAD, ph = (int) Math.round(pw * PHOTO_RATIO);
        Dimension d = photo.getPreferredSize();
        if (d != null && d.width == pw && d.height == ph) return;   // unchanged: no extra layout work
        photo.setPreferredSize(new Dimension(pw, ph));
        photo.setMinimumSize(new Dimension(10, ph));
        photo.setMaximumSize(new Dimension(Integer.MAX_VALUE, ph));
    }

    private static JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel detail(String key, String value) {
        String v = (value == null || value.isEmpty()) ? "-" : value.replace("&", "&amp;").replace("<", "&lt;");
        JLabel l = new JLabel("<html><span style='color:#696E7E'>" + key + "</span>&nbsp;<span style='color:#222430'>" + v + "</span></html>");
        l.setFont(CustomerTheme.body(14));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static BufferedImage load(Vehicle v) {
        try {
            if (v.getImageFilename() != null) return ImageUtil.loadFromDisk(IMAGE_FOLDER, v.getImageFilename());
        } catch (Exception ignored) { }
        return null;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = CustomerTheme.smooth(g);
        int w = getWidth() - 1, h = getHeight() - 1;
        g2.setColor(CustomerTheme.CARD_FILL);
        g2.fillRoundRect(0, 0, w, h, 22, 22);
        g2.setColor(hover ? CustomerTheme.GOLD : CustomerTheme.CARD_EDGE);
        g2.setStroke(new BasicStroke(hover ? 2f : 1f));
        g2.drawRoundRect(0, 0, w, h, 22, 22);
        g2.dispose();
        super.paintComponent(g);
    }
}
