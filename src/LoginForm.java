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
        setTitle("Rexter the molester Rentals — Login");
        setSize(420, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        UITheme.styleFrame(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = UITheme.titleLabel("Rexter the molester Rentals");
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
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginButton, gbc);

        JButton registerButton = new JButton("Create New Account (Customer)");
        registerButton.setBackground(UITheme.BG_PANEL);
        registerButton.setForeground(UITheme.TEXT_LIGHT);
        registerButton.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        loginButton.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin()); // Enter key submits
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
