package ui;

import util.UITheme;
import model.Staff;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Sidebar + CardLayout shell. Each menu button just swaps a panel in/out —
 * add a new module by writing a JPanel (see VehiclePanel) and adding one addModule(...) line below.
 * (Look: white sidebar with line icons, soft-blue selected item, page header with the signed-in user.)
 */
public class MainFrame extends JFrame {

    private static final Map<String, String> ICONS = new HashMap<>();
    static {
        ICONS.put("Dashboard", "dashboard");
        ICONS.put("Vehicles", "car");
        ICONS.put("Customer Accounts", "users");
        ICONS.put("Reservations", "calendar");
        ICONS.put("Rental Checkout", "key");
        ICONS.put("Vehicle Returns", "return");
        ICONS.put("Payment History", "card");
        ICONS.put("Stock & Supplies", "box");
        ICONS.put("Expenses", "wallet");
        ICONS.put("Reports & Analytics", "chart");
        ICONS.put("Vehicle Categories", "tag");
        ICONS.put("Discount Events", "percent");
        ICONS.put("Staff Accounts", "shield");
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JMenu modulesMenu = new JMenu("Modules");
    private final Map<String, NavButton> navButtons = new HashMap<>();
    private final Staff staff;
    private NavButton selectedSidebarButton;

    public MainFrame(Staff loggedInStaff) {
        this.staff = loggedInStaff;
        setTitle("Restro Rentals — " + loggedInStaff.getRole().toUpperCase() + " (" + loggedInStaff.getFirstName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1310, 940);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        UITheme.styleFrame(this);
        content.setBackground(UITheme.BG_DARK);

        boolean isAdmin = "admin".equalsIgnoreCase(loggedInStaff.getRole());

        // Menu bar: File (Log Out / Exit), Modules (one item per screen the user may open), Help
        modulesMenu.setMnemonic(java.awt.event.KeyEvent.VK_M);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(AppMenu.fileMenu(this));
        menuBar.add(modulesMenu);
        menuBar.add(HelpActions.buildHelpMenu(this));   // Test Database Connection + About
        setJMenuBar(menuBar);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_PANEL);
        sidebar.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
        sidebar.add(brand());
        sidebar.add(Box.createVerticalStrut(10));

        // Every module screen is registered here (Reports and the admin modules only for administrators).
        section(sidebar, "MAIN");
        DashboardPanel dashboard = new DashboardPanel("Welcome, " + loggedInStaff.getFirstName() + " (" + loggedInStaff.getRole() + ")");
        addModule(sidebar, "Dashboard", "Quick overview of vehicles, rentals, reservations, income, and expenses.", dashboard);
        addModule(sidebar, "Vehicles", "Manage the vehicle fleet and its car suppliers.",
            buildTabbedModule(
                new String[] {"Vehicle Fleet", "Vehicle Suppliers"},
                new String[] {
                    "Add and maintain vehicles, availability, rates, and photos.",
                    "Manage suppliers that provide vehicles to the fleet."
                },
                new JPanel[] { new VehiclePanel(loggedInStaff), new SuppliersPanel(loggedInStaff, model.Supplier.TYPE_CAR) }
            ));
        dashboard.setOnViewFleet(() -> showModule("Vehicles"));
        addModule(sidebar, "Customer Accounts", "View and manage customer profiles and account details.",
            new CustomerPanel(loggedInStaff));
        section(sidebar, "OPERATIONS");
        addModule(sidebar, "Reservations", "Manage bookings, review customer requests, and approve or reject pending reservations.",
            buildTabbedModule(
                new String[] {"Reservations", "Pending Reservations"},
                new String[] {
                    "View and manage all bookings, including dates, customer, vehicle, and reservation status.",
                    "Review customer-submitted requests. Approve a request before checking out a vehicle."
                },
                new JPanel[] { new ReservationsPanel(loggedInStaff), new PendingOrdersPanel() }
            ));
        addModule(sidebar, "Rental Checkout", "Check out vehicles for approved reservations or walk-in customers; manage active rentals.",
            new RentalsPanel(loggedInStaff));
        addModule(sidebar, "Vehicle Returns", "Record returned vehicles, condition notes, fees, and final payment.",
            new ReturnsPanel(loggedInStaff));
        section(sidebar, "BUSINESS");
        addModule(sidebar, "Payment History", "Review payments recorded for completed rental transactions.",
            new PaymentsPanel(loggedInStaff));
        addModule(sidebar, "Stock & Supplies", "Receive stock, manage supply items and reorder thresholds, and maintain supplier details.",
            buildTabbedModule(
                new String[] {"Stock Receipts", "Supplies", "Suppliers"},
                new String[] {
                    "Record incoming stock and review quantities currently on hand.",
                    "Maintain supply names, units, reorder thresholds, and stock levels.",
                    "Maintain supplier contact details used by the supplies list."
                },
                new JPanel[] { new InventoryPanel(loggedInStaff), new SuppliesPanel(loggedInStaff), new SuppliersPanel(loggedInStaff) }
            ));
        addModule(sidebar, "Expenses", "Record and review operating costs such as maintenance, fuel, and utilities.",
            new ExpensesPanel(loggedInStaff));

        // Reports is admin-only — employees don't see it in the sidebar at all.
        if (isAdmin) {
            section(sidebar, "ADMIN");
            addModule(sidebar, "Reports & Analytics", "Review income summaries, charts, and detailed business reports.",
                buildTabbedModule(
                    new String[] {"Summary Reports", "Detailed Reports"},
                    new String[] {
                        "View income by vehicle category and calculate income for a selected date range.",
                        "Generate focused reports for customers, vehicles, rentals, inventory, and transactions."
                    },
                    new JPanel[] { new ReportsPanel(), new DetailedReportsPanel() }
                ));
            addModule(sidebar, "Vehicle Categories", "Organize vehicles into categories used across the fleet.",
                new CategoriesPanel(loggedInStaff));
            addModule(sidebar, "Discount Events", "Create and manage date-based discounts applied to reservations and rentals.",
                new DiscountEventsPanel(loggedInStaff));
            addModule(sidebar, "Staff Accounts", "Manage staff logins, roles, and account access.",
                new StaffPanel(loggedInStaff));
        }

        sidebar.add(Box.createVerticalGlue());

        // The menu has many modules, so it scrolls; the Log Out and Exit buttons stay pinned at the bottom.
        JScrollPane navScroll = new JScrollPane(sidebar,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setBorder(null);
        navScroll.getViewport().setBackground(UITheme.BG_PANEL);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);

        NavButton logout = new NavButton("Log Out", "logout");
        logout.setDanger(true);
        logout.addActionListener(e -> AppMenu.logout(this));
        NavButton exit = new NavButton("Exit", "power");
        exit.addActionListener(e -> AppMenu.exit(this));
        JPanel logoutBar = new JPanel();
        logoutBar.setLayout(new BoxLayout(logoutBar, BoxLayout.Y_AXIS));
        logoutBar.setBackground(UITheme.BG_PANEL);
        logoutBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 12, 14)));
        logoutBar.add(logout);
        logoutBar.add(Box.createVerticalStrut(4));
        logoutBar.add(exit);

        JPanel sidebarWrap = new JPanel(new BorderLayout());
        sidebarWrap.setBackground(UITheme.BG_PANEL);
        sidebarWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER));
        sidebarWrap.setPreferredSize(new Dimension(250, 0));
        sidebarWrap.add(navScroll, BorderLayout.CENTER);
        sidebarWrap.add(logoutBar, BorderLayout.SOUTH);

        add(sidebarWrap, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        updateSelectedSidebarButton("Dashboard");
    }

    // ------------------------------------------------------------------ pages

    private JPanel buildTabbedModule(String[] tabLabels, String[] descriptions, JPanel[] tabPanels) {
        if (tabLabels.length != tabPanels.length || tabLabels.length != descriptions.length) {
            throw new IllegalArgumentException("Tab labels, descriptions, and panels must match in count.");
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < tabLabels.length; i++) {
            tabbedPane.addTab(tabLabels[i], tabPage(descriptions[i], tabPanels[i]));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        wrapper.add(tabbedPane, BorderLayout.CENTER);
        return wrapper;
    }

    /** One tab: a short description line, then the panel. */
    private JPanel tabPage(String description, JPanel body) {
        JPanel page = new JPanel(new BorderLayout(0, 6));
        page.setBackground(UITheme.BG_DARK);
        JLabel descriptionLabel = new JLabel("<html><div style='width:760px'>" + description + "</div></html>");
        descriptionLabel.setForeground(UITheme.TEXT_MUTED);
        descriptionLabel.setFont(UITheme.font(Font.PLAIN, 13));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(12, 4, 2, 4));
        page.add(descriptionLabel, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /** A screen: big title + description on the left, signed-in user and today's date on the right, then the panel. */
    private JPanel pageContent(String title, String description, JPanel body) {
        JPanel page = new JPanel(new BorderLayout(0, 6));
        page.setBackground(UITheme.BG_DARK);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);
        titleLabel.setFont(UITheme.font(Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descriptionLabel = new JLabel("<html><div style='width:640px'>" + description + "</div></html>");
        descriptionLabel.setForeground(UITheme.TEXT_MUTED);
        descriptionLabel.setFont(UITheme.font(Font.PLAIN, 13));
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(2));
        heading.add(descriptionLabel);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));
        top.add(heading, BorderLayout.CENTER);
        top.add(userChip(), BorderLayout.EAST);

        page.add(top, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JComponent userChip() {
        String first = staff.getFirstName() == null ? "" : staff.getFirstName();
        String last = staff.getLastName() == null ? "" : staff.getLastName();
        String initials = ((first.isEmpty() ? "" : first.substring(0, 1)) + (last.isEmpty() ? "" : last.substring(0, 1))).toUpperCase();
        if (initials.isEmpty()) initials = "?";
        final String ini = initials;
        JComponent avatar = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(UITheme.NAVY);
                g2.fillOval(0, 0, 40, 40);
                g2.setColor(Color.WHITE);
                g2.setFont(UITheme.font(Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, (40 - fm.stringWidth(ini)) / 2, (40 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(40, 40));

        String role = staff.getRole() == null ? "" : staff.getRole();
        role = role.isEmpty() ? "" : Character.toUpperCase(role.charAt(0)) + role.substring(1);
        JLabel name = new JLabel((first + " " + last).trim());
        name.setFont(UITheme.font(Font.BOLD, 13));
        name.setForeground(UITheme.TEXT_LIGHT);
        JLabel sub = new JLabel(role + "  \u00B7  " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")));
        sub.setFont(UITheme.font(Font.PLAIN, 12));
        sub.setForeground(UITheme.TEXT_MUTED);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(name);
        text.add(sub);
        text.add(Box.createVerticalGlue());

        JPanel chip = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                g2.setColor(UITheme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 18));
        chip.add(avatar, BorderLayout.WEST);
        chip.add(text, BorderLayout.CENTER);
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(chip);
        return holder;
    }

    // ------------------------------------------------------------------ sidebar

    private JComponent brand() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 0));
        java.net.URL logoUrl = MainFrame.class.getResource("/resources/RESTRO_LOGO.png");
        if (logoUrl == null) throw new IllegalStateException("Missing system logo: /resources/RESTRO_LOGO.png");
        Image image = new ImageIcon(logoUrl).getImage().getScaledInstance(38, 38, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(image));
        logo.setPreferredSize(new Dimension(38, 38));
        JLabel name = new JLabel("Restro Rentals");
        name.setFont(UITheme.font(Font.BOLD, 20));
        name.setForeground(UITheme.NAVY);
        JLabel sub = new JLabel("Vehicle rental system");
        sub.setFont(UITheme.font(Font.PLAIN, 11));
        sub.setForeground(UITheme.TEXT_MUTED);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(name);
        text.add(sub);
        p.add(logo, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        return p;
    }

    private void section(JPanel sidebar, String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.font(Font.BOLD, 11));
        l.setForeground(new Color(160, 170, 186));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(12, 10, 4, 0));
        sidebar.add(l);
    }

    private void addModule(JPanel sidebar, String label, String description, JPanel panel) {
        content.add(pageContent(label, description, panel), label);
        NavButton btn = new NavButton(label, ICONS.getOrDefault(label, "dashboard"));
        btn.setToolTipText(description);
        btn.setName(label);
        navButtons.put(label, btn);
        btn.addActionListener(e -> {
            cards.show(content, label);
            updateSelectedSidebarButton(btn);
        });
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            cards.show(content, label);
            updateSelectedSidebarButton(btn);
        });
        modulesMenu.add(item);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private void showModule(String label) {
        cards.show(content, label);
        updateSelectedSidebarButton(label);
    }

    private void updateSelectedSidebarButton(NavButton newSelectedButton) {
        if (selectedSidebarButton != null && selectedSidebarButton != newSelectedButton) {
            selectedSidebarButton.setSelectedItem(false);
        }
        selectedSidebarButton = newSelectedButton;
        selectedSidebarButton.setSelectedItem(true);
    }

    private void updateSelectedSidebarButton(String selectedLabel) {
        NavButton b = navButtons.get(selectedLabel);
        if (b != null) updateSelectedSidebarButton(b);
    }

    /** One sidebar entry: line icon + label; the selected one gets the soft-blue pill. */
    private static final class NavButton extends JButton {
        private final String icon;
        private boolean selected, danger;

        NavButton(String text, String icon) {
            super(text);
            this.icon = icon;
            setFont(UITheme.font(Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 46, 0, 8));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(100, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
            putClientProperty("ui.flat", Boolean.TRUE);
        }

        void setSelectedItem(boolean s) { selected = s; repaint(); }
        void setDanger(boolean d) { danger = d; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover();
            if (selected) { g2.setColor(UITheme.TINT); g2.fillRoundRect(0, 0, w, h, 14, 14); }
            else if (hover) { g2.setColor(new Color(244, 247, 251)); g2.fillRoundRect(0, 0, w, h, 14, 14); }
            if (selected) { g2.setColor(UITheme.RED_ACCENT); g2.fillRoundRect(0, 9, 4, h - 18, 4, 4); }
            Color c = danger ? UITheme.RED_ACCENT : selected ? UITheme.RED_ACCENT : UITheme.TEXT_MUTED;
            Icons.paint(icon, g2, 14, (h - 20) / 2f, 20, c);
            g2.setFont(selected ? getFont().deriveFont(Font.BOLD) : getFont());
            g2.setColor(danger ? UITheme.RED_ACCENT : selected ? UITheme.TEXT_LIGHT : new Color(70, 84, 106));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 46, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
