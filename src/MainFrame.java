import javax.swing.*;
import java.awt.*;

/**
 * Sidebar + CardLayout shell. Each menu button just swaps a panel in/out —
 * add a new module by writing a JPanel (see VehiclePanel) and adding two lines below.
 */
public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    public MainFrame(Staff loggedInStaff) {
        setTitle("Ocean Crest Rentals — " + loggedInStaff.role.toUpperCase() + " (" + loggedInStaff.firstName + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        UITheme.styleFrame(this);

        boolean isAdmin = "admin".equalsIgnoreCase(loggedInStaff.role);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.BG_PANEL);
        sidebar.setBorder(BorderFactory.createEmptyBorder(16, 10, 10, 10));
        sidebar.setPreferredSize(new Dimension(190, 0));

        JLabel brand = new JLabel("OCEAN CREST");
        brand.setForeground(UITheme.RED_ACCENT);
        brand.setFont(UITheme.FONT_TITLE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(16));

        // Register every module screen here. Replace the placeholder panels
        // with real ones the same way VehiclePanel was built.
        addModule(sidebar, "Vehicles", new VehiclePanel());
        addModule(sidebar, "Customers", new CustomerPanel());
        addModule(sidebar, "Inventory", new InventoryPanel());
        addModule(sidebar, "Supplies", new SuppliesPanel());
        addModule(sidebar, "Expenses", new ExpensesPanel(loggedInStaff));
        addModule(sidebar, "Reservations", new PendingOrdersPanel());
        addModule(sidebar, "Rentals / Transactions", new RentalsPanel(loggedInStaff));

        // Reports is admin-only — employees don't see it in the sidebar at all.
        if (isAdmin) {
            addModule(sidebar, "Reports", new ReportsPanel());
            addModule(sidebar, "Manage Staff", new StaffPanel(loggedInStaff));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton logout = UITheme.primaryButton("Log Out");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        logout.addActionListener(e -> { new LoginForm().setVisible(true); dispose(); });
        sidebar.add(logout);

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    private void addModule(JPanel sidebar, String label, JPanel panel) {
        content.add(panel, label);
        JButton btn = UITheme.sidebarButton(label);
        btn.addActionListener(e -> cards.show(content, label));
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private JPanel placeholder(String moduleName) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_DARK);
        JLabel l = new JLabel(moduleName + " — build this next, same pattern as VehiclePanel.", SwingConstants.CENTER);
        l.setForeground(UITheme.TEXT_MUTED);
        p.add(l, BorderLayout.CENTER);
        return p;
    }
}
