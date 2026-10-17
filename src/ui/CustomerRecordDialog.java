package ui;

import dao.CustomerDAO;
import dao.UniqueValueChecker;
import model.Customer;
import util.PasswordHasher;
import util.Validator;

import javax.swing.*;
import java.awt.*;

final class CustomerRecordDialog {
    private CustomerRecordDialog() { }

    static Customer prompt(Component parent, CustomerDAO customerDAO, UniqueValueChecker uniqueValues) {
        JTextField username = new JTextField(18);
        JTextField firstName = new JTextField(18);
        JTextField lastName = new JTextField(18);
        JTextField email = new JTextField(18);
        JTextField phone = new JTextField(18);
        JTextField license = new PlaceholderTextField(18, "ex. D01-23-456789");

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.add(new JLabel("Username:"));
        fields.add(username);
        fields.add(new JLabel("First name:"));
        fields.add(firstName);
        fields.add(new JLabel("Last name:"));
        fields.add(lastName);
        fields.add(new JLabel("Email (optional):"));
        fields.add(email);
        fields.add(new JLabel("Phone (optional, 11 digits):"));
        fields.add(phone);
        fields.add(new JLabel("Driver's license (required):"));
        fields.add(license);

        while (true) {
            int result = JOptionPane.showConfirmDialog(parent, fields, "New System Customer Record",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return null;

            String enteredUsername = username.getText().trim();
            String enteredFirstName = firstName.getText().trim();
            String enteredLastName = lastName.getText().trim();
            String enteredEmail = blankToNull(email.getText());
            String enteredPhone = blankToNull(phone.getText());
            String enteredLicense = blankToNull(license.getText());

            if (enteredUsername.isEmpty() || enteredFirstName.isEmpty() || enteredLastName.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Username, first name, and last name are required.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (enteredLicense == null) {
                JOptionPane.showMessageDialog(parent, "Driver's license number is required.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (!Validator.isValidEmail(enteredEmail)) {
                JOptionPane.showMessageDialog(parent, "Enter a valid email address or leave it blank.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (!Validator.isValidPhone(enteredPhone)) {
                JOptionPane.showMessageDialog(parent, "Phone number must be exactly 11 digits or left blank.",
                    "Invalid Phone", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (customerDAO.usernameExists(enteredUsername)) {
                JOptionPane.showMessageDialog(parent, "That username is already taken.",
                    "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            if ((enteredEmail != null && uniqueValues.customerEmailExists(enteredEmail, 0))
                    || uniqueValues.customerLicenseExists(enteredLicense, 0)) {
                JOptionPane.showMessageDialog(parent, "That email or driver's license is already registered.",
                    "Duplicate Customer Information", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            Customer customer = new Customer();
            customer.setUsername(enteredUsername);
            customer.setFirstName(enteredFirstName);
            customer.setLastName(enteredLastName);
            customer.setEmail(enteredEmail);
            customer.setPhone(enteredPhone);
            customer.setLicenseNumber(enteredLicense);
            customer.setActive(true);
            String temporaryPassword = randomTemporaryPassword();
            customer.setPasswordHash(PasswordHasher.hash(temporaryPassword));
            customerDAO.insert(customer);
            JOptionPane.showMessageDialog(parent,
                "System customer record added. Temporary password: " + temporaryPassword
                    + "\nThe customer should change it after logging in.",
                "Customer Added", JOptionPane.INFORMATION_MESSAGE);
            return customer;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String randomTemporaryPassword() {
        String characters = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) password.append(characters.charAt(random.nextInt(characters.length())));
        return password.toString();
    }
}
