import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * One "car dealership" card: photo on top, dark name bar + red price badge on
 * the bottom, matching the look of the reference screenshot. Used in the
 * customer's Browse & Reserve grid.
 */
public class VehicleCardPanel extends JPanel {

    private static final int CARD_WIDTH = 230;
    private static final int IMAGE_HEIGHT = 140;
    private static final String IMAGE_FOLDER = "VehicleImages";

    public VehicleCardPanel(Vehicle vehicle, Runnable onReserveClicked) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(CARD_WIDTH, IMAGE_HEIGHT + 70));
        setBackground(UITheme.CARD_BG);
        setBorder(new LineBorder(new Color(60, 60, 60), 1));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(CARD_WIDTH, IMAGE_HEIGHT));
        imageLabel.setBackground(Color.DARK_GRAY);
        imageLabel.setOpaque(true);
        loadThumbnail(vehicle, imageLabel);
        add(imageLabel, BorderLayout.CENTER);

        // Bottom bar: dark strip with name, category tag, and a red price badge — dealership style.
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(20, 20, 20));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel nameLabel = new JLabel(vehicle.name.toUpperCase());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(UITheme.FONT_HEADER);
        bottomBar.add(nameLabel, BorderLayout.WEST);

        JLabel priceLabel = new JLabel(UITheme.peso(vehicle.dailyRate) + "/day", SwingConstants.CENTER);
        priceLabel.setOpaque(true);
        priceLabel.setBackground(UITheme.RED_ACCENT);
        priceLabel.setForeground(Color.WHITE);
        priceLabel.setFont(UITheme.FONT_PRICE);
        priceLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bottomBar.add(priceLabel, BorderLayout.EAST);

        JLabel categoryTag = new JLabel(vehicle.categoryName);
        categoryTag.setForeground(UITheme.TEXT_MUTED);
        categoryTag.setFont(UITheme.FONT_BODY);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(new Color(20, 20, 20));
        south.add(bottomBar, BorderLayout.NORTH);
        south.add(categoryTag, BorderLayout.SOUTH);

        if (onReserveClicked != null) {
            JButton reserveBtn = UITheme.primaryButton("Reserve");
            reserveBtn.addActionListener(e -> onReserveClicked.run());
            JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            btnWrap.setBackground(new Color(20, 20, 20));
            btnWrap.add(reserveBtn);
            south.add(btnWrap, BorderLayout.CENTER);
        }

        add(south, BorderLayout.SOUTH);
    }

    private void loadThumbnail(Vehicle vehicle, JLabel target) {
        try {
            BufferedImage img = ImageUtil.loadFromDisk(IMAGE_FOLDER, vehicle.imageFilename);
            if (img != null) {
                target.setIcon(new ImageIcon(ImageUtil.coverFit(img, CARD_WIDTH, IMAGE_HEIGHT)));
                return;
            }
        } catch (Exception ignored) { }
        target.setText("No Photo");
        target.setForeground(Color.LIGHT_GRAY);
        target.setBackground(new Color(45, 45, 45));
    }
}
