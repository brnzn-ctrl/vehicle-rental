import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginForm extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    // Hardcoded credentials for demo purposes
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password123";

    public LoginForm() {
        setTitle("Login Form");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username label + field
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(usernameField, gbc);

        // Password label + field
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(passwordField, gbc);

        // Login button
        JButton loginButton = new JButton("Login");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        // Allow pressing Enter in password field to submit
        passwordField.addActionListener(e -> handleLogin());

        add(panel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
            JOptionPane.showMessageDialog(this,
                    "Login successful! Welcome, " + username + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            // Proceed to next screen here
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginForm form = new LoginForm();
            form.setVisible(true);
        });
    }
}
