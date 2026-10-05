package ui;

import dao.DiscountEventDAO;
import model.DiscountEvent;
import model.Vehicle;
import util.CustomerTheme;
import util.ImageUtil;
import util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.function.BiFunction;

/**
 * Reserve window: vehicle photo, rates, two date pickers and a live price estimate.
 * The rules (not in the past, end after start, no duplicate, vehicle free) come in as `validator`
 * (returns an error text or null) and are shown inline, so the window stays open until the dates are valid.
 */
/** The content of the Reserve window (a plain panel, so it can also be tested without opening a window). */
class ReserveForm extends JPanel {

    private LocalDate resultStart, resultEnd;

    private final JSpinner fromSpinner, toSpinner;
    private final JLabel lblEstimate = new JLabel(" ");
    private final JLabel lblDeal = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final Vehicle vehicle;
    private final DiscountEventDAO discountDAO;
    private final BiFunction<LocalDate, LocalDate, String> validator;

    private final JButton confirmButton = CustomerTheme.pill("Confirm Reservation", true);
    private final Runnable onClose;

    LocalDate[] result() { return resultStart == null ? null : new LocalDate[]{resultStart, resultEnd}; }
    JButton confirmButton() { return confirmButton; }

    ReserveForm(Vehicle v, DiscountEventDAO discountDAO,
                BiFunction<LocalDate, LocalDate, String> validator, Runnable onClose) {
        super(new BorderLayout(0, 14));
        this.onClose = onClose;
        this.vehicle = v;
        this.discountDAO = discountDAO;
        this.validator = validator;

        Date today = toDate(LocalDate.now());
        fromSpinner = dateSpinner(today);
        toSpinner = dateSpinner(toDate(LocalDate.now().plusDays(1)));

        JPanel root = this;
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // top: photo + title
        BufferedImage img = null;
        try { if (v.getImageFilename() != null) img = ImageUtil.loadFromDisk("VehicleImages", v.getImageFilename()); }
        catch (Exception ignored) { }
        PhotoPanel photo = new PhotoPanel(img, 16);
        photo.setPreferredSize(new Dimension(420, 190));

        JLabel cat = text(v.getCategoryName() == null ? "" : v.getCategoryName(), CustomerTheme.italic(13), CustomerTheme.MUTED);
        JLabel name = text(v.getName(), CustomerTheme.script(26), CustomerTheme.TEXT);
        JLabel rate = text(UITheme.peso(v.getDailyRate()) + " / day", CustomerTheme.bold(17), CustomerTheme.GOLD);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(cat); title.add(name); title.add(Box.createVerticalStrut(2)); title.add(rate);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(photo, BorderLayout.NORTH);
        top.add(title, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        // middle: dates + estimate
        JPanel dates = new JPanel(new GridLayout(2, 2, 10, 8));
        dates.setOpaque(false);
        dates.add(text("Start date", CustomerTheme.bold(14), CustomerTheme.MUTED));
        dates.add(text("End date", CustomerTheme.bold(14), CustomerTheme.MUTED));
        dates.add(fromSpinner);
        dates.add(toSpinner);

        lblEstimate.setFont(CustomerTheme.bold(16)); lblEstimate.setForeground(CustomerTheme.TEXT);
        lblDeal.setFont(CustomerTheme.body(14));     lblDeal.setForeground(CustomerTheme.GOLD);
        lblError.setFont(CustomerTheme.bold(13));    lblError.setForeground(CustomerTheme.RED);

        dates.setAlignmentX(LEFT_ALIGNMENT);
        lblEstimate.setAlignmentX(LEFT_ALIGNMENT);
        lblDeal.setAlignmentX(LEFT_ALIGNMENT);
        lblError.setAlignmentX(LEFT_ALIGNMENT);
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.add(dates);
        mid.add(Box.createVerticalStrut(12));
        mid.add(lblEstimate); mid.add(Box.createVerticalStrut(3));
        mid.add(lblDeal);     mid.add(Box.createVerticalStrut(6));
        mid.add(lblError);
        root.add(mid, BorderLayout.CENTER);

        // bottom: buttons
        JButton cancel = CustomerTheme.pill("Cancel", false);
        JButton confirm = confirmButton;
        cancel.addActionListener(e -> onClose.run());
        confirm.addActionListener(e -> confirm());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        bottom.add(cancel); bottom.add(confirm);
        root.add(bottom, BorderLayout.SOUTH);

        fromSpinner.addChangeListener(e -> refresh());
        toSpinner.addChangeListener(e -> refresh());
        refresh();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = CustomerTheme.smooth(g);
        g2.setPaint(new GradientPaint(0, 0, CustomerTheme.BG_TOP, 0, getHeight(), CustomerTheme.BG_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private void refresh() {
        LocalDate s = toLocal((Date) fromSpinner.getValue()), e = toLocal((Date) toSpinner.getValue());
        lblError.setText(" ");
        if (e.isBefore(s)) {
            lblEstimate.setText("Pick an end date on or after the start date");
            lblDeal.setText(" ");
            return;
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(s, e));
        BigDecimal subtotal = vehicle.getDailyRate().multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        DiscountEvent deal = null;
        try { deal = discountDAO.findActiveForDate(s); } catch (RuntimeException ignored) { }
        if (deal != null) {
            BigDecimal off = subtotal.multiply(deal.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            lblEstimate.setText(days + (days == 1 ? " day" : " days") + "  \u00B7  estimated " + UITheme.peso(subtotal.subtract(off)));
            lblDeal.setText("\u2605 " + deal.getEventName() + ": " + deal.getDiscountPercent().stripTrailingZeros().toPlainString() + "% off applied");
        } else {
            lblEstimate.setText(days + (days == 1 ? " day" : " days") + "  \u00B7  estimated " + UITheme.peso(subtotal));
            lblDeal.setText(" ");
        }
    }

    private void confirm() {
        LocalDate s = toLocal((Date) fromSpinner.getValue()), e = toLocal((Date) toSpinner.getValue());
        String problem;
        try { problem = validator.apply(s, e); }
        catch (RuntimeException ex) { problem = util.ErrorHandler.friendlyMessage(ex); }
        if (problem != null) {
            lblError.setText("<html>" + problem + "</html>");
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.pack();                    // grow the window to fit the message
            return;
        }
        resultStart = s; resultEnd = e;
        onClose.run();
    }

    // ---- small helpers
    private static JLabel text(String t, Font f, Color c) {
        JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }

    private static JSpinner dateSpinner(Date value) {
        JSpinner sp = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor ed = new JSpinner.DateEditor(sp, "MMM dd, yyyy");
        sp.setEditor(ed);
        sp.setFont(CustomerTheme.bold(16));
        JTextField tf = ed.getTextField();
        tf.setFont(CustomerTheme.bold(16));
        tf.setBackground(Color.WHITE);
        tf.setForeground(CustomerTheme.TEXT);
        tf.setCaretColor(CustomerTheme.GOLD);
        tf.setHorizontalAlignment(JTextField.CENTER);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 60)));
        return sp;
    }

    private static LocalDate toLocal(Date d) { return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); }
    private static Date toDate(LocalDate d) { return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()); }
}
