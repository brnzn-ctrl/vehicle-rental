package ui;

import dao.DiscountEventDAO;
import dao.ReservationDAO;
import dao.VehicleCategoryDAO;
import dao.VehicleDAO;
import model.Customer;
import model.DiscountEvent;
import model.Reservation;
import model.Vehicle;
import model.VehicleCategory;
import util.CustomerTheme;
import util.ErrorHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer portal, restyled as a dark "showroom": top navigation, search + category chips, and a responsive grid of
 * glass vehicle cards (see VehicleCardPanel). Second view = the customer's own reservations (table).
 * Logic is unchanged: SQL search, duplicate / overlap checks, pending -> staff approval, cancel with confirmation.
 */
public class CustomerFrame extends JFrame {

    private final Customer customer;
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final VehicleCategoryDAO categoryDAO = new VehicleCategoryDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final DiscountEventDAO discountDAO = new DiscountEventDAO();

    private final CardLayout views = new CardLayout();
    private final JPanel viewHost = new JPanel(views);
    private static final String BROWSE = "browse", HISTORY = "history";

    private final CardGridPanel grid = new CardGridPanel();
    private final JLabel lblCount = new JLabel(" ");
    private final CustomerWidgets.SearchField txtVehicleSearch = new CustomerWidgets.SearchField("Search by name, plate, category or price", 26);
    private final JPanel chipRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 6));
    private final ButtonGroup chipGroup = new ButtonGroup();
    private String vehicleKeyword = "";
    private int categoryFilter = 0;          // 0 = all categories
    private Timer searchDelay;

    private final DefaultTableModel historyModel =
        new DefaultTableModel(new Object[]{"ID", "Vehicle", "From", "To", "Status", "Discount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel lblHistoryCount = new JLabel(" ");
    private final CustomerWidgets.SearchField txtHistorySearch = new CustomerWidgets.SearchField("Search my reservations", 22);
    private String historyKeyword = "";

    public CustomerFrame(Customer customer) {
        this.customer = customer;
        setTitle("Restro Rentals \u2014 Customer Portal (" + customer.getFirstName() + ")");
        setSize(1180, 780);
        setMinimumSize(new Dimension(820, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        CustomerWidgets.Backdrop root = new CustomerWidgets.Backdrop();
        setContentPane(root);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(AppMenu.fileMenu(this));
        menuBar.add(AppMenu.helpMenu(this));
        setJMenuBar(menuBar);

        viewHost.setOpaque(false);
        viewHost.add(buildBrowseView(), BROWSE);
        viewHost.add(buildHistoryView(), HISTORY);
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(viewHost, BorderLayout.CENTER);

        ErrorHandler.run(this, () -> { buildChips(); refreshVehicles(); refreshHistory(); });
    }

    // ------------------------------------------------------------------ top bar
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 28, 0, 28));

        JLabel brand = new JLabel("<html><span style='color:#B88C14'>RESTRO</span> <span style='color:#696E7E;font-size:11px'>RENTALS</span></html>");
        brand.setFont(CustomerTheme.bold(22));
        bar.add(brand, BorderLayout.WEST);

        CustomerWidgets.NavTab tabBrowse = new CustomerWidgets.NavTab("Browse & Reserve");
        CustomerWidgets.NavTab tabHistory = new CustomerWidgets.NavTab("My Reservations");
        ButtonGroup g = new ButtonGroup();
        g.add(tabBrowse); g.add(tabHistory);
        tabBrowse.setSelected(true);
        tabBrowse.addActionListener(e -> { views.show(viewHost, BROWSE); ErrorHandler.run(this, this::refreshVehicles); });
        tabHistory.addActionListener(e -> { views.show(viewHost, HISTORY); ErrorHandler.run(this, this::refreshHistory); });
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        nav.setOpaque(false);
        nav.add(tabBrowse); nav.add(tabHistory);
        bar.add(nav, BorderLayout.CENTER);

        JLabel hello = new JLabel("Hi, " + customer.getFirstName());
        hello.setFont(CustomerTheme.body(15));
        hello.setForeground(CustomerTheme.MUTED);
        JButton logout = CustomerTheme.pill("Log Out", false);
        logout.addActionListener(e -> AppMenu.logout(this));
        JButton exit = CustomerTheme.pill("Exit", false);
        exit.addActionListener(e -> AppMenu.exit(this));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(hello); right.add(logout); right.add(exit);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------------ browse view
    private JPanel buildBrowseView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);

        JLabel title = new JLabel("Find your next ride");
        title.setFont(CustomerTheme.script(34));
        title.setForeground(CustomerTheme.TEXT);
        JLabel sub = new JLabel("Choose from our top-end vehicles at great prices");
        sub.setFont(CustomerTheme.body(15));
        sub.setForeground(CustomerTheme.MUTED);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title); heading.add(sub);

        lblCount.setFont(CustomerTheme.bold(14));
        lblCount.setForeground(CustomerTheme.GOLD);

        txtVehicleSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (searchDelay != null) searchDelay.stop();
                searchDelay = new Timer(250, e -> {                       // wait for a pause in typing, then query
                    vehicleKeyword = txtVehicleSearch.getText().trim();
                    ErrorHandler.run(CustomerFrame.this, CustomerFrame.this::refreshVehicles);
                });
                searchDelay.setRepeats(false);
                searchDelay.start();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        });

        chipRow.setOpaque(false);
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        searchRow.setOpaque(false);
        searchRow.add(txtVehicleSearch);
        searchRow.add(lblCount);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createEmptyBorder(14, 28, 10, 28));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        chipRow.setAlignmentX(LEFT_ALIGNMENT);
        head.add(heading);
        head.add(Box.createVerticalStrut(12));
        head.add(searchRow);
        head.add(Box.createVerticalStrut(4));
        head.add(chipRow);
        view.add(head, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);   // transparent view: avoid scroll artefacts
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        view.add(scroll, BorderLayout.CENTER);

        // keep the columns right when the window is resized
        scroll.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { grid.revalidate(); }
        });
        return view;
    }

    /** One chip per vehicle category plus "All"; clicking a chip filters the cards. */
    private void buildChips() {
        chipRow.removeAll();
        addChip("All", 0, true);
        for (VehicleCategory c : categoryDAO.findAll()) addChip(c.getName(), c.getId(), false);
        chipRow.revalidate();
        chipRow.repaint();
    }

    private void addChip(String text, int categoryId, boolean selected) {
        CustomerWidgets.Chip chip = new CustomerWidgets.Chip(text);
        chip.setSelected(selected);
        chip.addActionListener(e -> { categoryFilter = categoryId; ErrorHandler.run(this, this::refreshVehicles); });
        chipGroup.add(chip);
        chipRow.add(chip);
    }

    private void refreshVehicles() {
        grid.removeAll();
        BigDecimal deal = null;
        DiscountEvent today = discountDAO.findActiveForDate(LocalDate.now());
        if (today != null) deal = today.getDiscountPercent();

        int shown = 0;
        for (Vehicle v : vehicleDAO.search(vehicleKeyword)) {              // SQL: WHERE ... LIKE ?
            if (!"available".equals(v.getStatus())) continue;
            if (categoryFilter != 0 && v.getCategoryId() != categoryFilter) continue;
            final Vehicle vehicle = v;
            grid.add(new VehicleCardPanel(v, deal, () -> ErrorHandler.run(this, () -> openReserveDialog(vehicle))));
            shown++;
        }
        if (shown == 0) {
            JLabel none = new JLabel("No vehicles match your search right now.", SwingConstants.CENTER);
            none.setFont(CustomerTheme.script(22));
            none.setForeground(CustomerTheme.MUTED);
            none.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            grid.add(none);
        }
        lblCount.setText(shown + (shown == 1 ? " vehicle available" : " vehicles available"));
        grid.revalidate();
        grid.repaint();
    }

    // ------------------------------------------------------------------ reservation history view
    private JPanel buildHistoryView() {
        JPanel view = new JPanel(new BorderLayout(0, 14));
        view.setOpaque(false);
        view.setBorder(BorderFactory.createEmptyBorder(14, 28, 24, 28));

        JLabel title = new JLabel("My Reservations");
        title.setFont(CustomerTheme.script(34));
        title.setForeground(CustomerTheme.TEXT);

        JButton btnSearch = CustomerTheme.pill("Search", true);
        JButton btnReset = CustomerTheme.pill("Reset", false);
        JButton btnCancel = CustomerTheme.pill("Cancel Selected", false);
        JButton btnRefresh = CustomerTheme.pill("Refresh", false);
        lblHistoryCount.setFont(CustomerTheme.bold(14));
        lblHistoryCount.setForeground(CustomerTheme.GOLD);

        Runnable apply = () -> { historyKeyword = txtHistorySearch.getText().trim(); refreshHistory(); };
        btnSearch.addActionListener(e -> ErrorHandler.run(this, apply));
        txtHistorySearch.addActionListener(e -> ErrorHandler.run(this, apply));
        btnReset.addActionListener(e -> { txtHistorySearch.setText(""); ErrorHandler.run(this, apply); });
        btnCancel.addActionListener(e -> ErrorHandler.run(this, this::cancelSelectedReservation));
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, this::refreshHistory));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tools.setOpaque(false);
        tools.add(txtHistorySearch); tools.add(btnSearch); tools.add(btnReset);
        tools.add(Box.createHorizontalStrut(14));
        tools.add(btnCancel); tools.add(btnRefresh); tools.add(lblHistoryCount);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        tools.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title); head.add(Box.createVerticalStrut(10)); head.add(tools);
        view.add(head, BorderLayout.NORTH);

        historyTable.setAutoCreateRowSorter(true);                          // click a heading to sort
        CustomerWidgets.styleTable(historyTable, "Status");
        view.add(CustomerWidgets.scroll(historyTable), BorderLayout.CENTER);
        return view;
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (Reservation r : reservationDAO.searchByCustomer(customer.getId(), historyKeyword)) {
            historyModel.addRow(new Object[]{
                r.getId(), r.getVehicleName(), r.getStartDate(), r.getEndDate(), r.getStatus(),
                r.getDiscountEventName() != null ? r.getDiscountEventName() : "\u2014"
            });
        }
        lblHistoryCount.setText(historyKeyword.isEmpty() ? " " : historyModel.getRowCount() + " record(s) found");
    }

    // ------------------------------------------------------------------ reserve
    private void openReserveDialog(Vehicle vehicle) {
        LocalDate[] picked = ReserveDialog.ask(this, vehicle, discountDAO, (start, end) -> validateDates(vehicle, start, end));
        if (picked == null) return;
        LocalDate start = picked[0], end = picked[1];

        DiscountEvent discount = discountDAO.findActiveForDate(start);

        Reservation r = new Reservation();
        r.setCustomerId(customer.getId());
        r.setVehicleId(vehicle.getId());
        r.setStartDate(start);
        r.setEndDate(end);
        r.setStatus("pending");
        r.setDiscountId((discount != null) ? discount.getId() : null);
        reservationDAO.insert(r);

        String discountMsg = (discount != null)
            ? "\n\uD83C\uDF89 " + discount.getEventName() + " discount applied: " + discount.getDiscountPercent() + "% off!"
            : "";
        JOptionPane.showMessageDialog(this, "Reservation submitted! Staff will review and approve it." + discountMsg);
        refreshHistory();
    }

    /** The rules from the requirements (dates, duplicates, availability). Returns the problem text, or null when OK. */
    private String validateDates(Vehicle vehicle, LocalDate start, LocalDate end) {
        if (start.isBefore(LocalDate.now())) return "Start date cannot be in the past.";
        if (end.isBefore(start)) return "End date cannot be before the start date.";
        if (reservationDAO.customerHasOverlap(customer.getId(), vehicle.getId(), start, end))
            return "You already have a pending or approved reservation for this vehicle on those dates.";
        if (reservationDAO.vehicleBookedBetween(vehicle.getId(), start, end, 0))
            return "Sorry, this vehicle is already booked for those dates. Please choose other dates.";
        return null;
    }

    /** Customer can withdraw a reservation that staff has not approved yet (UPDATE status = cancelled). */
    private void cancelSelectedReservation() {
        int row = historyTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation first."); return; }
        int id = (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(row), 0);
        Reservation r = reservationDAO.findById(id);
        if (r == null || r.getCustomerId() != customer.getId()) {
            JOptionPane.showMessageDialog(this, "That reservation no longer exists.");
            refreshHistory();
            return;
        }
        if (!"pending".equals(r.getStatus())) {
            JOptionPane.showMessageDialog(this, "Only pending reservations can be cancelled.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel this reservation?",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        r.setStatus("cancelled");
        reservationDAO.update(r);
        JOptionPane.showMessageDialog(this, "Reservation cancelled.");
        refreshHistory();
    }
}
