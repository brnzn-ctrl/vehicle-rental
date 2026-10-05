package ui;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.Component;
import java.sql.*;

/** Help menu: "Test Database Connection" (for the defense, section 43-B) and "About". */
public final class HelpActions {

    private static final String[] TABLES = {
        "VEHICLE_CATEGORIES", "STAFF", "CUSTOMERS", "SUPPLIERS", "DISCOUNT_EVENTS", "VEHICLES", "SUPPLIES",
        "RESERVATIONS", "RENTALS", "RETURNS", "PAYMENTS", "INVENTORY_STOCK", "EXPENSES"
    };

    private HelpActions() { }

    /** Add this menu to the main window's JMenuBar: menuBar.add(HelpActions.buildHelpMenu(this)); */
    public static JMenu buildHelpMenu(Component parent) {
        JMenu help = new JMenu("Help");
        JMenuItem test = new JMenuItem("Test Database Connection");
        test.addActionListener(e -> testDatabaseConnection(parent));
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(parent,
                "Restro Rentals - Vehicle Rental Management System\n"
              + "Java Swing / AWT  |  JDBC  |  Apache Derby",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(test);
        help.add(about);
        return help;
    }

    /** Opens a JDBC connection to Derby and shows the URL, version and number of rows in every table. */
    public static void testDatabaseConnection(Component parent) {
        try (Connection c = DatabaseConnection.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            StringBuilder sb = new StringBuilder();
            sb.append("CONNECTED\n\n");
            sb.append("Database: ").append(md.getDatabaseProductName()).append(' ')
              .append(md.getDatabaseProductVersion()).append('\n');
            sb.append("Driver:   ").append(md.getDriverName()).append('\n');
            sb.append("URL:      ").append(md.getURL()).append("\n\n");
            sb.append("Rows per table:\n");
            for (String t : TABLES) {
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    rs.next();
                    sb.append(String.format("  %-20s %d%n", t, rs.getInt(1)));
                } catch (SQLException ex) {
                    sb.append(String.format("  %-20s (missing)%n", t));
                }
            }
            JTextArea area = new JTextArea(sb.toString(), 20, 48);
            area.setEditable(false);
            area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
            JOptionPane.showMessageDialog(parent, new JScrollPane(area),
                    "Database Connection Test", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent,
                    "Could not connect to the Derby database.\n" + e.getMessage(),
                    "Database Connection Test", JOptionPane.ERROR_MESSAGE);
        }
    }
}
