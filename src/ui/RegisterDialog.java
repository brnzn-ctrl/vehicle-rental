package ui;

import util.PasswordHasher;
import util.Validator;
import dao.CustomerDAO;
import model.Customer;
import javax.swing.*;
import java.awt.*;

/** Customer self-registration. Enforces unique username with a real DB check before insert. */
public class RegisterDialog extends JDialog {

    public String registeredUsername; // set on success, so LoginForm can prefill it

    private final CustomerDAO customerDAO;
    private final JTextField txtUsername = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JPasswordField txtConfirm = new JPasswordField(15);
    private final JTextField txtFirstName = new JTextField(15);
    private final JTextField txtLastName = new JTextField(15);
    private final JTextField txtEmail = new JTextField(15);
    private final JTextField txtPhone = new JTextField(15);
    private final JTextField txtLicense = new JTextField(15);

    public RegisterDialog(Frame owner, CustomerDAO customerDAO) {
        super(owner, "Create Customer Account", true);
        this.customerDAO = customerDAO;
        setSize(400, 420);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gc, 0, "Username:", txtUsername);
        addRow(form, gc, 1, "Password:", txtPassword);
        addRow(form, gc, 2, "Confirm Password:", txtConfirm);
        addRow(form, gc, 3, "First Name:", txtFirstName);
        addRow(form, gc, 4, "Last Name:", txtLastName);
        addRow(form, gc, 5, "Email:", txtEmail);
        addRow(form, gc, 6, "Phone (11 digits):", txtPhone);
        addRow(form, gc, 7, "Driver's License No.:", txtLicense);

        JButton btnCreate = new JButton("Create Account");
        btnCreate.addActionListener(e -> attemptRegister());
        gc.gridx = 0; gc.gridy = 8; gc.gridwidth = 2; gc.anchor = GridBagConstraints.CENTER;
        form.add(btnCreate, gc);

        add(form);
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    private void attemptRegister() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirm  = new String(txtConfirm.getPassword());
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String license = txtLicense.getText().trim();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, password, first and last name are required.");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address (e.g. name@example.com).");
            return;
        }
        if (!Validator.isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 11 digits.");
            return;
        }

        // Systematic uniqueness check — this is what stops two different people
        // from registering the same username.
        if (customerDAO.usernameExists(username)) {
            JOptionPane.showMessageDialog(this,
                    "That username is already taken. Please choose another.",
                    "Username Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Customer c = new Customer();
        c.setUsername(username);
        c.setPasswordHash(PasswordHasher.hash(password));
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email.isEmpty() ? null : email);
        c.setPhone(phone.isEmpty() ? null : phone);
        c.setLicenseNumber(license.isEmpty() ? null : license);
        c.setActive(true);

        try {
            customerDAO.insert(c);
            registeredUsername = username;
            JOptionPane.showMessageDialog(this, "Account created! You can now log in.");
            dispose();
        } catch (RuntimeException ex) {
            // Covers the rare race condition where two people submit the same
            // username at nearly the same instant — the DB's UNIQUE constraint
            // catches what the earlier check might miss.
            JOptionPane.showMessageDialog(this,
                    "Could not create account — that username or email/license may already be in use.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
