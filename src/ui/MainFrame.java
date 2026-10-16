package ui;

import util.UITheme;
import model.Staff;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Sidebar + CardLayout shell. Each menu button just swaps a panel in/out —
 * add a new module by writing a JPanel (see VehiclePanel) and adding one addModule(...) line below.
 */
public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JMenu modulesMenu = new JMenu("Modules");
    private JButton selectedSidebarButton;

    public MainFrame(Staff loggedInStaff) {
        setTitle("Restro Rentals — " + loggedInStaff.getRole().toUpperCase() + " (" + loggedInStaff.getFirstName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1310, 940);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        UITheme.styleFrame(this);

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
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 10, 10, 16));

        JLabel brand = new JLabel("RESTRO RENTALS");
        brand.setForeground(UITheme.RED_ACCENT);
        brand.setFont(UITheme.FONT_TITLE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(16));

        // Every module screen is registered here (Reports and the admin modules only for administrators).
        addModule(sidebar, "Dashboard", "Quick overview of vehicles, rentals, reservations, income, and expenses.",
            new DashboardPanel("Welcome, " + loggedInStaff.getFirstName() + " (" + loggedInStaff.getRole() + ")"));
        addModule(sidebar, "Vehicle Fleet", "Add and maintain vehicles, availability, rates, and photos.",
            new VehiclePanel(loggedInStaff));
        addModule(sidebar, "Customer Accounts", "View and manage customer profiles and account details.",
            new CustomerPanel(loggedInStaff));
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

        // The menu now has many modules, so it scrolls; the Log Out and Exit buttons stay pinned at the bottom.
        JScrollPane navScroll = new JScrollPane(sidebar,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setBorder(null);
        navScroll.getViewport().setBackground(UITheme.BG_PANEL);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton logout = UITheme.primaryButton("Log Out");
        logout.addActionListener(e -> AppMenu.logout(this));
        JButton exit = UITheme.primaryButton("Exit");
        exit.addActionListener(e -> AppMenu.exit(this));
        JPanel logoutBar = new JPanel(new GridLayout(2, 1, 0, 6));
        logoutBar.setBackground(UITheme.BG_PANEL);
        logoutBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        logoutBar.add(logout);
        logoutBar.add(exit);

        JPanel sidebarWrap = new JPanel(new BorderLayout());
        sidebarWrap.setBackground(UITheme.BG_PANEL);
        sidebarWrap.setPreferredSize(new Dimension(285, 0));
        sidebarWrap.add(navScroll, BorderLayout.CENTER);
        sidebarWrap.add(logoutBar, BorderLayout.SOUTH);

        add(sidebarWrap, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        updateSelectedSidebarButton("Dashboard");
    }

    private JPanel buildTabbedModule(String[] tabLabels, String[] descriptions, JPanel[] tabPanels) {
        if (tabLabels.length != tabPanels.length || tabLabels.length != descriptions.length) {
            throw new IllegalArgumentException("Tab labels, descriptions, and panels must match in count.");
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < tabLabels.length; i++) {
            tabbedPane.addTab(tabLabels[i], pageContent(tabLabels[i], descriptions[i], tabPanels[i]));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.add(tabbedPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel pageContent(String title, String description, JPanel body) {
        JPanel page = new JPanel(new BorderLayout(0, 10));
        page.setBackground(UITheme.BG_DARK);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setBackground(UITheme.BG_DARK);
        heading.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UITheme.TEXT_LIGHT);
        titleLabel.setFont(UITheme.FONT_TITLE.deriveFont(26f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descriptionLabel = new JLabel("<html><div style='width:900px'>" + description + "</div></html>");
        descriptionLabel.setForeground(UITheme.TEXT_MUTED);
        descriptionLabel.setFont(UITheme.FONT_BODY.deriveFont(15f));
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(4));
        heading.add(descriptionLabel);

        page.add(heading, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void addModule(JPanel sidebar, String label, String description, JPanel panel) {
        content.add(pageContent(label, description, panel), label);
        JButton btn = UITheme.sidebarButton(label);
        btn.setToolTipText(description);
        btn.setName(label);
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
        sidebar.add(Box.createVerticalStrut(4));
    }

    private void updateSelectedSidebarButton(JButton newSelectedButton) {
        if (selectedSidebarButton != null && selectedSidebarButton != newSelectedButton) {
            selectedSidebarButton.setBackground(UITheme.BG_PANEL);
            selectedSidebarButton.setOpaque(false);
            selectedSidebarButton.setBorder(new EmptyBorder(6, 12, 6, 12));
        }

        selectedSidebarButton = newSelectedButton;
        selectedSidebarButton.setBackground(new Color(8, 45, 110));
        selectedSidebarButton.setOpaque(true);
        selectedSidebarButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 190, 255), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        selectedSidebarButton.repaint();
    }

    private void updateSelectedSidebarButton(String selectedLabel) {
        for (Component comp : ((Container) getContentPane().getComponent(0)).getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                Container view = (Container) scroll.getViewport().getView();
                for (Component child : view.getComponents()) {
                    if (child instanceof JButton && selectedLabel.equals(child.getName())) {
                        updateSelectedSidebarButton((JButton) child);
                        return;
                    }
                }
            }
        }
    }
}
