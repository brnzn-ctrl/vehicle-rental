package ui;

import dao.DashboardDAO;
import dao.DashboardInsightsDAO;
import dao.VehicleDAO;
import model.Vehicle;
import util.ImageUtil;
import util.ModernUI;
import util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Dashboard: stat cards, earnings chart, rental-status ring, fleet photos, car types and recent rentals.
 * The summary numbers still come from DashboardDAO.summary() (unchanged); the extra chart data comes from the
 * read-only DashboardInsightsDAO and the fleet photos from VehicleDAO.findAll().
 */
public class DashboardPanel extends JPanel {
    private static final String IMAGE_FOLDER = "VehicleImages";
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final DashboardDAO dao = new DashboardDAO();
    private final DashboardInsightsDAO insights = new DashboardInsightsDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final String welcomeText;
    private final JPanel body = new ScrollBody();
    private final Map<String, BufferedImage> photoCache = new HashMap<>();
    private Runnable onViewFleet;

    public DashboardPanel(String welcomeText) {
        this.welcomeText = welcomeText;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_DARK);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(4, 20, 20, 20));

        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        load();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { load(); }
        });
    }

    /** Lets the window turn the "View all" link into a jump to the Vehicle Fleet screen. */
    public void setOnViewFleet(Runnable r) { this.onViewFleet = r; }

    // ------------------------------------------------------------------ data + layout

    private void load() {
        Map<String, String> s = dao.summary();
        photoCache.clear();
        List<Vehicle> vehicles = vehicleDAO.findAll();
        int year = LocalDate.now().getYear();
        Map<Integer, BigDecimal> income = insights.incomeByMonth(year);
        Map<Integer, BigDecimal> expense = insights.expensesByMonth(year);
        Map<String, Integer> rentalStatus = insights.rentalsByStatus();
        List<Object[]> recent = insights.recentRentals(5);

        body.removeAll();
        body.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;

        // ---- greeting row
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; gc.weightx = 1; gc.weighty = 0;
        body.add(greeting(), gc);

        // ---- stat cards across the full width
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 1; gc.weighty = 0;
        gc.insets = new Insets(0, 0, 14, 0);
        body.add(statRow(s), gc);

        // ---- left column
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(chartsRow(income, expense, rentalStatus, year, s));
        left.add(Box.createVerticalStrut(14));
        left.add(fleetShowcase(vehicles));
        left.add(Box.createVerticalStrut(14));
        left.add(recentRentals(recent));
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 1; gc.weightx = 1; gc.weighty = 1;
        gc.insets = new Insets(0, 0, 0, 14);
        body.add(left, gc);

        // ---- right column
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(fleetStatus(s));
        right.add(Box.createVerticalStrut(14));
        right.add(carTypes(vehicles));
        right.add(Box.createVerticalStrut(14));
        right.add(reminders(s));
        right.setPreferredSize(new Dimension(300, 10));
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 0; gc.insets = new Insets(0, 0, 0, 0);
        body.add(right, gc);

        body.revalidate();
        body.repaint();
    }

    private JComponent greeting() {
        JLabel title = new JLabel("Restro Rentals \u2014 Vehicle Rental System");
        title.setFont(UITheme.font(Font.BOLD, 20));
        title.setForeground(UITheme.TEXT_LIGHT);
        JLabel welcome = new JLabel(welcomeText);
        welcome.setFont(UITheme.font(Font.PLAIN, 14));
        welcome.setForeground(UITheme.TEXT_MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(welcome);

        JButton refresh = new JButton("Refresh", Icons.icon("refresh", 16, UITheme.TEXT_LIGHT));
        refresh.setIconTextGap(8);
        refresh.addActionListener(e -> load());

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        row.add(text, BorderLayout.CENTER);
        row.add(refresh, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------ stat cards

    private JComponent statRow(Map<String, String> s) {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.add(statCard("peso", "Total Income", money(s.get("Total Income (PHP)")), "All recorded payments", UITheme.RED_ACCENT));
        row.add(statCard("wallet", "Total Expenses", money(s.get("Total Expenses (PHP)")), "All recorded expenses", UITheme.NAVY));
        row.add(statCard("card", "Today's Payments", money(s.get("Today's Payments (PHP)")), "Collected today", UITheme.GREEN_MONEY));
        row.add(statCard("users", "Customers", num(s.get("Customers")), "Active accounts", UITheme.BLUE));
        row.setPreferredSize(new Dimension(100, 104));
        return row;
    }

    private JComponent statCard(String icon, String label, String value, String note, Color accent) {
        Card card = new Card(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 14));
        JComponent badge = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Icons.badge(icon, g2, 0, 0, 44, tint(accent), accent);
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(44, 44));
        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel l = label(label, 12, Font.PLAIN, UITheme.TEXT_MUTED);
        JLabel v = label(value, 21, Font.BOLD, UITheme.TEXT_LIGHT);
        JLabel n = label(note, 11, Font.PLAIN, UITheme.TEXT_MUTED);
        text.add(Box.createVerticalGlue());
        text.add(l); text.add(Box.createVerticalStrut(2)); text.add(v); text.add(Box.createVerticalStrut(2)); text.add(n);
        text.add(Box.createVerticalGlue());
        card.add(badgeWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------ charts

    private JComponent chartsRow(Map<Integer, BigDecimal> income, Map<Integer, BigDecimal> expense,
                                 Map<String, Integer> status, int year, Map<String, String> s) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH; gc.gridy = 0; gc.weighty = 1;

        // earnings summary (line chart: income as red area, expenses as navy line)
        Card earnings = new Card(new BorderLayout());
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setOpaque(false);
        legend.add(dot("Income", UITheme.RED_ACCENT));
        legend.add(dot("Expenses", UITheme.NAVY));
        legend.add(Card.chip(String.valueOf(year)));
        earnings.add(Card.header("Earnings Summary", legend), BorderLayout.NORTH);
        int upTo = LocalDate.now().getYear() == year ? LocalDate.now().getMonthValue() : 12;   // do not draw months that have not happened yet
        double[] inc = new double[upTo], exp = new double[upTo];
        for (int m = 1; m <= upTo; m++) {
            inc[m - 1] = income.getOrDefault(m, BigDecimal.ZERO).doubleValue();
            exp[m - 1] = expense.getOrDefault(m, BigDecimal.ZERO).doubleValue();
        }
        LineChartPanel chart = new LineChartPanel();
        chart.setData(MONTHS, List.of(
            new LineChartPanel.Series("Expenses", UITheme.NAVY, exp, false),
            new LineChartPanel.Series("Income", UITheme.RED_ACCENT, inc, true)));
        chart.setPreferredSize(new Dimension(300, 250));
        earnings.add(chart, BorderLayout.CENTER);
        gc.gridx = 0; gc.weightx = 1; gc.insets = new Insets(0, 0, 0, 14);
        row.add(earnings, gc);

        // rent status ring
        Card ring = new Card(new BorderLayout());
        ring.add(Card.header("Rent Status", Card.chip("All time")), BorderLayout.NORTH);
        DonutChartPanel donut = new DonutChartPanel();
        donut.setData(List.of(
            new DonutChartPanel.Slice("Ongoing", status.getOrDefault("ongoing", 0), UITheme.RED_ACCENT),
            new DonutChartPanel.Slice("Returned", status.getOrDefault("returned", 0), UITheme.NAVY),
            new DonutChartPanel.Slice("Cancelled", status.getOrDefault("cancelled", 0), new Color(203, 213, 225))), "Rentals");
        ring.add(donut, BorderLayout.CENTER);
        ring.setPreferredSize(new Dimension(290, 10));
        gc.gridx = 1; gc.weightx = 0; gc.insets = new Insets(0, 0, 0, 0);
        row.add(ring, gc);

        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(100, 330));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
        return row;
    }

    // ------------------------------------------------------------------ fleet photos

    private JComponent fleetShowcase(List<Vehicle> vehicles) {
        Card card = new Card(new BorderLayout());
        JLabel all = label("View all", 12, Font.BOLD, UITheme.RED_ACCENT);
        all.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        all.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (onViewFleet != null) onViewFleet.run(); }
        });
        card.add(Card.header("Our Fleet", onViewFleet == null ? Card.chip(vehicles.size() + " vehicles") : all), BorderLayout.NORTH);

        List<Vehicle> pick = new ArrayList<>(vehicles);
        pick.sort(Comparator.comparing((Vehicle v) -> photo(v) == null));      // vehicles that have a photo come first
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        int shown = Math.min(4, pick.size());
        for (int i = 0; i < 4; i++) grid.add(i < shown ? vehicleTile(pick.get(i)) : emptyTile(i == 0 && pick.isEmpty()));
        card.add(grid, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 232));
        card.setPreferredSize(new Dimension(100, 232));
        return card;
    }

    private JComponent vehicleTile(Vehicle v) {
        JPanel tile = new JPanel();
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        PhotoPanel photo = new StatusPhoto(photo(v), v.getStatus());
        photo.setPreferredSize(new Dimension(100, 96));
        photo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        photo.setAlignmentX(LEFT_ALIGNMENT);
        tile.add(photo);
        tile.add(Box.createVerticalStrut(8));
        tile.add(left(label(v.getName(), 14, Font.BOLD, UITheme.TEXT_LIGHT)));
        tile.add(left(label(v.getCategoryName() == null ? "" : v.getCategoryName(), 12, Font.PLAIN, UITheme.TEXT_MUTED)));
        tile.add(Box.createVerticalStrut(6));
        tile.add(left(label(UITheme.peso(v.getDailyRate()) + " / day", 13, Font.BOLD, UITheme.RED_ACCENT)));
        return tile;
    }

    private JComponent emptyTile(boolean first) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        if (first) p.add(label("No vehicles yet. Add one in Vehicle Fleet.", 13, Font.PLAIN, UITheme.TEXT_MUTED));
        return p;
    }

    private BufferedImage photo(Vehicle v) {
        String f = v.getImageFilename();
        if (f == null || f.isEmpty()) return null;
        if (photoCache.containsKey(f)) return photoCache.get(f);
        BufferedImage img = null;
        try { img = ImageUtil.loadFromDisk(IMAGE_FOLDER, f); } catch (Exception ignored) { }
        photoCache.put(f, img);
        return img;
    }

    // ------------------------------------------------------------------ recent rentals

    private JComponent recentRentals(List<Object[]> rows) {
        Card card = new Card(new BorderLayout());
        card.add(Card.header("Recent Rentals", Card.chip("Latest " + Math.max(rows.size(), 0))), BorderLayout.NORTH);
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Rental", "Customer", "Vehicle", "Rented Out", "Due", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        SimpleDateFormat out = new SimpleDateFormat("MMM d");
        for (Object[] r : rows) {
            model.addRow(new Object[] { "RN-" + r[0], r[1], r[2], out.format((java.util.Date) r[3]), out.format((java.util.Date) r[4]), r[6] });
        }
        JTable table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        ModernUI.polishTable(table);
        int[] widths = {60, 140, 150, 80, 80, 110};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new ModernUI.RoundBorder(UITheme.BORDER, 12));
        wrap.add(table.getTableHeader(), BorderLayout.NORTH);
        if (rows.isEmpty()) {
            JLabel empty = label("No rentals yet. They appear here after the first checkout.", 13, Font.PLAIN, UITheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
            wrap.add(empty, BorderLayout.CENTER);
        } else {
            wrap.add(table, BorderLayout.CENTER);
        }
        card.add(wrap, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20 + 64 + 40 + Math.max(1, rows.size()) * 39 + 26));
        return card;
    }

    // ------------------------------------------------------------------ right column

    private JComponent fleetStatus(Map<String, String> s) {
        int total = parse(s.get("Total Vehicles")), avail = parse(s.get("Available Vehicles")), rented = parse(s.get("Rented Vehicles"));
        int other = Math.max(0, total - avail - rented);
        Card card = new Card(new BorderLayout());
        card.add(Card.header("Fleet Status", Card.chip(total + " total")), BorderLayout.NORTH);

        JComponent bar = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = 12;
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.setColor(new Color(236, 241, 247));
                g2.fillRect(0, 0, w, h);
                if (total > 0) {
                    int x = 0;
                    int[] vals = {avail, rented, other};
                    Color[] cols = {UITheme.GREEN_MONEY, UITheme.RED_ACCENT, new Color(203, 213, 225)};
                    for (int i = 0; i < 3; i++) {
                        int part = (int) Math.round(w * (double) vals[i] / total);
                        g2.setColor(cols[i]);
                        g2.fillRect(x, 0, i == 2 ? w - x : part, h);
                        x += part;
                    }
                }
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(100, 12));
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        col.add(bar);
        col.add(Box.createVerticalStrut(12));
        col.add(legendRow("Available", avail, UITheme.GREEN_MONEY));
        col.add(legendRow("Rented", rented, UITheme.RED_ACCENT));
        col.add(legendRow("Maintenance / inactive", other, new Color(203, 213, 225)));
        card.add(col, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        return card;
    }

    private JComponent carTypes(List<Vehicle> vehicles) {
        Map<String, Integer> count = new LinkedHashMap<>();
        Map<String, BufferedImage> img = new HashMap<>();
        for (Vehicle v : vehicles) {
            String c = v.getCategoryName() == null ? "Other" : v.getCategoryName();
            count.merge(c, 1, Integer::sum);
            if (photo(v) != null && !img.containsKey(c)) img.put(c, photo(v));
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(count.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        Card card = new Card(new BorderLayout());
        card.add(Card.header("Car Types", Card.chip(count.size() + " categories")), BorderLayout.NORTH);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        int shown = Math.min(5, sorted.size());
        for (int i = 0; i < shown; i++) {
            Map.Entry<String, Integer> e = sorted.get(i);
            col.add(typeRow(e.getKey(), e.getValue(), vehicles.size(), img.get(e.getKey()), i == 0));
            if (i < shown - 1) col.add(Box.createVerticalStrut(8));
        }
        if (sorted.isEmpty()) col.add(label("No categories yet.", 13, Font.PLAIN, UITheme.TEXT_MUTED));
        card.add(col, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62 + 40 + shown * 68 + 20));
        return card;
    }

    private JComponent typeRow(String name, int n, int total, BufferedImage image, boolean highlight) {
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * n / total);
        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(highlight ? UITheme.NAVY : UITheme.BORDER);
                g2.setStroke(new BasicStroke(highlight ? 1.6f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));
        PhotoPanel photo = new PhotoPanel(image, 10);
        photo.setPreferredSize(new Dimension(68, 46));
        row.add(photo, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JPanel sub = new JPanel(new BorderLayout());
        sub.setOpaque(false);
        sub.add(label(n + (n == 1 ? " vehicle" : " vehicles"), 11, Font.PLAIN, UITheme.TEXT_MUTED), BorderLayout.WEST);
        sub.add(label(pct + "%", 11, Font.BOLD, UITheme.TEXT_LIGHT), BorderLayout.EAST);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        JComponent meter = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(236, 241, 247));
                g2.fillRoundRect(0, 0, getWidth(), 7, 7, 7);
                g2.setColor(highlight ? UITheme.RED_ACCENT : UITheme.NAVY);
                g2.fillRoundRect(0, 0, Math.max(7, getWidth() * pct / 100), 7, 7, 7);
                g2.dispose();
            }
        };
        meter.setPreferredSize(new Dimension(10, 8));
        meter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        meter.setAlignmentX(LEFT_ALIGNMENT);
        mid.add(Box.createVerticalGlue());
        mid.add(left(label(name, 13, Font.BOLD, UITheme.TEXT_LIGHT)));
        mid.add(Box.createVerticalStrut(2));
        mid.add(sub);
        mid.add(Box.createVerticalStrut(5));
        mid.add(meter);
        mid.add(Box.createVerticalGlue());
        row.add(mid, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.setPreferredSize(new Dimension(10, 60));
        return row;
    }

    private JComponent reminders(Map<String, String> s) {
        Card card = new Card(new BorderLayout());
        card.add(Card.header("Reminders", null), BorderLayout.NORTH);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Object[][] items = {
            {"Pending Reservations", "clock", UITheme.AMBER, "Pending Reservations"},
            {"Today's Rentals", "key", UITheme.BLUE, "Today's Rentals"},
            {"Ongoing Rentals", "car", UITheme.RED_ACCENT, "Ongoing Rentals"},
            {"Low-Stock Supplies", "alert", UITheme.AMBER, "Low-Stock Supplies"},
            {"Critical-Stock Supplies", "alert", new Color(229, 57, 77), "Critical-Stock Supplies"},
            {"System Users (Staff)", "shield", UITheme.NAVY, "System Users (Staff)"},
        };
        for (int i = 0; i < items.length; i++) {
            col.add(reminderRow((String) items[i][0], (String) items[i][1], (Color) items[i][2], num(s.get((String) items[i][3]))));
            if (i < items.length - 1) col.add(Box.createVerticalStrut(6));
        }
        card.add(col, BorderLayout.CENTER);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62 + 6 * 46 + 30));
        return card;
    }

    private JComponent reminderRow(String text, String icon, Color accent, String count) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(244, 248, 252));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 12));
        JComponent badge = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Icons.badge(icon, g2, 0, 0, 30, tint(accent), accent);
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(30, 30));
        row.add(badge, BorderLayout.WEST);
        row.add(label(text, 13, Font.PLAIN, UITheme.TEXT_LIGHT), BorderLayout.CENTER);
        row.add(label(count, 14, Font.BOLD, UITheme.TEXT_LIGHT), BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setPreferredSize(new Dimension(10, 40));
        return row;
    }

    // ------------------------------------------------------------------ small helpers

    private static JComponent legendRow(String text, int n, Color c) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillRoundRect(0, 4, 9, 9, 3, 3);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(11, 16));
        p.add(dot, BorderLayout.WEST);
        p.add(label(text, 13, Font.PLAIN, UITheme.TEXT_LIGHT), BorderLayout.CENTER);
        p.add(label(String.valueOf(n), 13, Font.BOLD, UITheme.TEXT_LIGHT), BorderLayout.EAST);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return p;
    }

    private static JComponent dot(String text, Color c) {
        JLabel l = label(text, 12, Font.PLAIN, UITheme.TEXT_MUTED);
        l.setIcon(new Icon() {
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y, 9, 9);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 9; }
            @Override public int getIconHeight() { return 9; }
        });
        l.setIconTextGap(6);
        return l;
    }

    private static JLabel pill(String status) {
        Color[] c = ModernUI.statusColors(status);
        String t = status == null || status.isEmpty() ? "" : Character.toUpperCase(status.charAt(0)) + status.substring(1);
        JLabel l = new JLabel(t) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c[0]);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(UITheme.font(Font.BOLD, 11));
        l.setForeground(c[1]);
        l.setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
        return l;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.font(style, size));
        l.setForeground(color);
        return l;
    }

    private static <T extends JComponent> T left(T c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    private static Color tint(Color c) {
        return new Color((int) (255 - (255 - c.getRed()) * 0.13), (int) (255 - (255 - c.getGreen()) * 0.13), (int) (255 - (255 - c.getBlue()) * 0.13));
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String num(String s) { return s == null ? "0" : s; }

    private static String money(String s) {
        try { return UITheme.peso(new BigDecimal(s.trim())); } catch (Exception e) { return s == null ? "\u20B10.00" : s; }
    }

    /** Vehicle photo with its status pill drawn in the top-left corner. */
    private static final class StatusPhoto extends PhotoPanel {
        private final String status;
        StatusPhoto(BufferedImage img, String status) { super(img, 14); this.status = status; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (status == null || status.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color[] c = ModernUI.statusColors(status);
            String t = Character.toUpperCase(status.charAt(0)) + status.substring(1);
            g2.setFont(UITheme.font(Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(t) + 18, h = 20;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(8, 8, w, h, h, h);
            g2.setColor(c[0]);
            g2.fillRoundRect(8, 8, w, h, h, h);
            g2.setColor(c[1]);
            g2.drawString(t, 17, 8 + (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Page body that follows the viewport width (so the columns fit) and scrolls only vertically. */
    private static final class ScrollBody extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
