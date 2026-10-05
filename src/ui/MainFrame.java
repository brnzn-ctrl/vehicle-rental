package ui;

import util.UITheme;
import model.Staff;
import javax.swing.*;
import java.awt.*;

/**
 * Sidebar + CardLayout shell. Each menu button just swaps a panel in/out —
 * add a new module by writing a JPanel (see VehiclePanel) and adding one addModule(...) line below.
 */
public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JMenu modulesMenu = new JMenu("Modules");

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
        addModule(sidebar, "Dashboard", new DashboardPanel(
            "Welcome, " + loggedInStaff.getFirstName() + " (" + loggedInStaff.getRole() + ")"));
        addModule(sidebar, "Vehicles", new VehiclePanel(loggedInStaff));
        addModule(sidebar, "Customers", new CustomerPanel(loggedInStaff));
        addModule(sidebar, "Reservations", new ReservationsPanel(loggedInStaff));
        addModule(sidebar, "Online Orders", new PendingOrdersPanel());       // approve / reject portal requests
        addModule(sidebar, "Rentals / Transactions", new RentalsPanel(loggedInStaff));
        addModule(sidebar, "Returns", new ReturnsPanel(loggedInStaff));
        addModule(sidebar, "Payments", new PaymentsPanel(loggedInStaff));
        addModule(sidebar, "Inventory", new InventoryPanel(loggedInStaff));
        addModule(sidebar, "Supplies", new SuppliesPanel(loggedInStaff));
        addModule(sidebar, "Suppliers", new SuppliersPanel(loggedInStaff));
        addModule(sidebar, "Expenses", new ExpensesPanel(loggedInStaff));

        // Reports is admin-only — employees don't see it in the sidebar at all.
        if (isAdmin) {
            addModule(sidebar, "Reports", new ReportsPanel());
            addModule(sidebar, "Detailed Reports", new DetailedReportsPanel());
            addModule(sidebar, "Vehicle Categories", new CategoriesPanel(loggedInStaff));
            addModule(sidebar, "Discount Events", new DiscountEventsPanel(loggedInStaff));
            addModule(sidebar, "Manage Staff", new StaffPanel(loggedInStaff));
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
    }

    private void addModule(JPanel sidebar, String label, JPanel panel) {
        content.add(panel, label);
        JButton btn = UITheme.sidebarButton(label);
        btn.addActionListener(e -> cards.show(content, label));
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> cards.show(content, label));
        modulesMenu.add(item);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(4));
    }
}
