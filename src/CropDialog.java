
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

/**
 * Modal dialog: shows the picture, user drags a rectangle, clicks "Crop & Use".
 * result is null if the user cancels.
 */
public class CropDialog extends JDialog {

    public BufferedImage result;
    private final BufferedImage source;
    private Point dragStart, dragEnd;

    public CropDialog(Window owner, BufferedImage source) {
        super(owner, "Crop Photo", ModalityType.APPLICATION_MODAL);
        this.source = source;

        JPanel canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(source, 0, 0, this);
                if (dragStart != null && dragEnd != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(255, 255, 0, 120));
                    g2.fill(rect());
                    g2.setColor(Color.YELLOW);
                    g2.draw(rect());
                }
            }
        };
        canvas.setPreferredSize(new Dimension(source.getWidth(), source.getHeight()));

        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragStart = e.getPoint(); dragEnd = e.getPoint(); canvas.repaint(); }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { dragEnd = e.getPoint(); canvas.repaint(); }
        });

        JButton crop = new JButton("Crop & Use");
        crop.addActionListener(e -> {
            result = (dragStart == null) ? source : ImageUtil.crop(source, rect());
            dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(crop);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private Rectangle rect() {
        int x = Math.min(dragStart.x, dragEnd.x);
        int y = Math.min(dragStart.y, dragEnd.y);
        int w = Math.abs(dragEnd.x - dragStart.x);
        int h = Math.abs(dragEnd.y - dragStart.y);
        return new Rectangle(x, y, w, h);
    }
}

