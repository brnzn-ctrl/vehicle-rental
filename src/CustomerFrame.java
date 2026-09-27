import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/** Customer-facing portal, styled like a car dealership site (see VehicleCardPanel). */
public class CustomerFrame extends JFrame {

    private final Customer customer;
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final DiscountEventDAO discountDAO = new DiscountEventDAO();

    private final JPanel cardGrid = new JPanel(new GridLayout(0, 4, 12, 12));

    private final DefaultTableModel historyModel =
        new DefaultTableModel(new Object[]{"Vehicle", "From", "To", "Status", "Discount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable historyTable = new JTable(historyModel);

    public CustomerFrame(Customer customer) {
        this.customer = customer;
        setTitle("Rexter the molester Rentals — Customer Portal (" + customer.firstName + ")");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        UITheme.styleFrame(this);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.BG_DARK);
        tabs.setForeground(UITheme.TEXT_LIGHT);
        tabs.addTab("Browse & Reserve Vehicles", buildBrowsePanel());
        tabs.addTab("My Reservation History", buildHistoryPanel());
        add(tabs, BorderLayout.CENTER);

        JButton logout = UITheme.primaryButton("Log Out");
        logout.addActionListener(e -> { new LoginForm().setVisible(true); dispose(); });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(UITheme.BG_DARK);
        bottom.add(logout);
        add(bottom, BorderLayout.SOUTH);

        refreshVehicles();
        refreshHistory();
    }

    private JPanel buildBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Color.BLACK);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel bannerText = new JLabel("--- CHOOSE FROM OUR TOP-END VEHICLES AT GREAT PRICES ---");
        bannerText.setForeground(UITheme.GREEN_MONEY);
        bannerText.setFont(UITheme.FONT_HEADER);
        bannerText.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(bannerText, BorderLayout.CENTER);
        panel.add(banner, BorderLayout.NORTH);

        cardGrid.setBackground(UITheme.BG_DARK);
        cardGrid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JScrollPane scroll = new JScrollPane(cardGrid);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        historyTable.setRowHeight(24);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshVehicles() {
        cardGrid.removeAll();
        List<Vehicle> vehicles = vehicleDAO.findAll();
        for (Vehicle v : vehicles) {
            if ("available".equals(v.status)) {
                cardGrid.add(new VehicleCardPanel(v, () -> openReserveDialog(v)));
            }
        }
        cardGrid.revalidate();
        cardGrid.repaint();
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (Reservation r : reservationDAO.findByCustomer(customer.id)) {
            historyModel.addRow(new Object[]{
                r.vehicleName, r.startDate, r.endDate, r.status,
                r.discountEventName != null ? r.discountEventName : "—"
            });
        }
    }

    private void openReserveDialog(Vehicle vehicle) {
        JSpinner fromSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner toSpinner = new JSpinner(new SpinnerDateModel());
        fromSpinner.setEditor(new JSpinner.DateEditor(fromSpinner, "yyyy-MM-dd"));
        toSpinner.setEditor(new JSpinner.DateEditor(toSpinner, "yyyy-MM-dd"));

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.add(new JLabel("Start Date:")); form.add(fromSpinner);
        form.add(new JLabel("End Date:"));   form.add(toSpinner);

        int result = JOptionPane.showConfirmDialog(this, form, "Reserve " + vehicle.name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        LocalDate start = toLocalDate((Date) fromSpinner.getValue());
        LocalDate end = toLocalDate((Date) toSpinner.getValue());
        if (end.isBefore(start)) { JOptionPane.showMessageDialog(this, "End date must be after start date."); return; }

        DiscountEvent discount = discountDAO.findActiveForDate(start);

        Reservation r = new Reservation();
        r.customerId = customer.id;
        r.vehicleId = vehicle.id;
        r.startDate = start;
        r.endDate = end;
        r.status = "pending";
        r.discountId = (discount != null) ? discount.id : null;
        reservationDAO.insert(r);

        String discountMsg = (discount != null)
            ? "\n\uD83C\uDF89 " + discount.eventName + " discount applied: " + discount.discountPercent + "% off!"
            : "";
        JOptionPane.showMessageDialog(this,
            "Reservation submitted! Staff will review and approve it." + discountMsg);
        refreshHistory();
    }

    private LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
