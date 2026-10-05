package ui;

import util.UITheme;
import util.ErrorHandler;
import util.PasswordHasher;
import database.SchemaInitializer;
import database.SchemaUpgrade;
import database.DatabaseConnection;
import dao.CustomerDAO;
import dao.StaffDAO;
import model.Staff;
import model.Customer;
import javax.swing.*;
import java.awt.*;

/**
 * One login screen, two audiences: pick "Staff" to log into the admin/employee
 * dashboard (checked against the `staff` table), or "Customer" to log into
 * the customer portal (checked against the `customers` table). Customers can
 * also self-register here — staff accounts stay admin-managed only.
 */
public class LoginForm extends JFrame {

    private final JComboBox<String> roleBox = new JComboBox<>(new String[]{"Staff", "Customer"});
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final StaffDAO staffDAO = new StaffDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public LoginForm() {
        setTitle("Restro Rentals — Login");
        setSize(420, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        UITheme.styleFrame(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = UITheme.titleLabel("RESTRO RENTALS");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(themedLabel("Login as:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(roleBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(themedLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(themedLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(passwordField, gbc);

        JButton loginButton = UITheme.primaryButton("Login");
        JButton clearButton = new JButton("Clear");
        JButton exitButton = new JButton("Exit");
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(loginButton);
        buttonRow.add(clearButton);
        buttonRow.add(exitButton);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonRow, gbc);

        JButton registerButton = new JButton("Create New Account (Customer)");
        registerButton.setBackground(UITheme.BG_PANEL);
        registerButton.setForeground(UITheme.TEXT_LIGHT);
        registerButton.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        loginButton.addActionListener(e -> ErrorHandler.run(this, this::handleLogin));
        clearButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            roleBox.setSelectedIndex(0);
            usernameField.requestFocus();
        });
        exitButton.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Exit the application?", "Exit", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) System.exit(0);
        });
        passwordField.addActionListener(e -> ErrorHandler.run(this, this::handleLogin)); // Enter key submits
        registerButton.addActionListener(e -> openRegisterDialog());

        add(panel);
    }

    private JLabel themedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UITheme.TEXT_LIGHT);
        return l;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ("Staff".equals(role)) {
            Staff loggedInStaff = staffDAO.login(username, password);
            if (loggedInStaff != null) {
                new MainFrame(loggedInStaff).setVisible(true);
                dispose();
                return;
            }
        } else {
            Customer loggedInCustomer = customerDAO.login(username, password);
            if (loggedInCustomer != null) {
                new CustomerFrame(loggedInCustomer).setVisible(true);
                dispose();
                return;
            }
        }

        JOptionPane.showMessageDialog(this,
                "Invalid username or password for the selected role.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
    }

    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, customerDAO);
        dialog.setVisible(true);
        if (dialog.registeredUsername != null) {
            usernameField.setText(dialog.registeredUsername);
            roleBox.setSelectedItem("Customer");
            passwordField.requestFocus();
        }
    }

    /**
     * Starts the application (called by Main.main): large readable fonts, friendly error dialog, creates / upgrades
     * the Derby tables, then shows the login screen.
     */
    public static void launch() {
        UITheme.applyGlobalFontScale();      // makes every Swing component's default text large/readable
        ErrorHandler.install();              // friendly dialog for any uncaught error
        try {
            SchemaInitializer.run();         // creates Derby tables on first run, seeds default admin
            String warning = SchemaUpgrade.run();   // widens password_hash so salted hashes fit (does nothing if already done)
            if (warning != null) {
                PasswordHasher.setSaltedEnabled(false);   // keep working with the old format instead of failing
                JOptionPane.showMessageDialog(null, warning, "Database Upgrade", JOptionPane.WARNING_MESSAGE);
            }
        } catch (RuntimeException ex) {      // e.g. database cannot be opened
            ErrorHandler.show(null, ex);
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown)); // cleanly closes Derby on exit
    }
}
