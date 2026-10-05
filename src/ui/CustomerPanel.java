package ui;

import util.PasswordHasher;
import util.Validator;
import util.ErrorHandler;
import dao.CustomerDAO;
import model.Customer;
import model.Staff;
import util.AccessControl;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Staff/Admin screen: list, add, edit, deactivate customer accounts. */
public class CustomerPanel extends JPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Username", "Name", "Email", "Phone", "License #", "Active"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JTextField txtUsername = new JTextField(15);
    private final JTextField txtFirstName = new JTextField(15);
    private final JTextField txtLastName = new JTextField(15);
    private final JTextField txtEmail = new JTextField(15);
    private final JTextField txtPhone = new JTextField(15);
    private final JTextField txtLicense = new JTextField(15);
    private final JCheckBox chkActive = new JCheckBox("Active", true);

    private Customer selected;
    private final dao.UniqueValueChecker uniqueValues = new dao.UniqueValueChecker();
    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use CustomerPanel(Staff). */
    public CustomerPanel() { this(null); }

    public CustomerPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        FormSupport.onDoubleClick(table, this::onRowSelected);   // MouseListener: double-click a row to load it into the form
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Username:", txtUsername);
        addField(form, gc, row++, "First Name:", txtFirstName);
        addField(form, gc, row++, "Last Name:", txtLastName);
        addField(form, gc, row++, "Email:", txtEmail);
        addField(form, gc, row++, "Phone:", txtPhone);
        addField(form, gc, row++, "License #:", txtLicense);

        gc.gridx = 1; gc.gridy = row++;
        form.add(chkActive, gc);

        JButton btnAdd = new JButton("Save");
        JButton btnUpdate = new JButton("Update");
        JButton btnResetPassword = new JButton("Reset Password");
        JButton btnDelete = new JButton("Delete");
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteCustomer));
        JButton btnClear = new JButton("Clear");
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addCustomer));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateCustomer));
        btnResetPassword.addActionListener(e -> ErrorHandler.run(this, this::resetPassword));
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnResetPassword);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        buttons.add(btnRefresh);
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        form.add(buttons, gc);

        return form;
    }

    private void addField(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Customer c : customerDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                c.getId(), c.getUsername(), c.getFirstName() + " " + c.getLastName(), c.getEmail(), c.getPhone(), c.getLicenseNumber(),
                c.isActive() ? "Yes" : "No"
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = customerDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());

        txtUsername.setText(selected.getUsername());
        txtFirstName.setText(selected.getFirstName());
        txtLastName.setText(selected.getLastName());
        txtEmail.setText(selected.getEmail());
        txtPhone.setText(selected.getPhone());
        txtLicense.setText(selected.getLicenseNumber());
        chkActive.setSelected(selected.isActive());
    }

    private void addCustomer() {
        Customer c = readForm(new Customer());
        if (c == null) return;

        if (customerDAO.usernameExists(c.getUsername())) {
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }
        if (duplicateContact(c)) return;

        String tempPassword = randomTempPassword();
        c.setPasswordHash(PasswordHasher.hash(tempPassword));
        customerDAO.insert(c);
        JOptionPane.showMessageDialog(this,
            "Customer added. Temporary password: " + tempPassword + "\n(Tell them to change it after logging in.)");
        clearForm();
        refreshTable();
    }

    private void updateCustomer() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        String oldUsername = selected.getUsername();   // remember it: readForm() overwrites selected.username
        Customer c = readForm(selected);
        if (c == null) { selected.setUsername(oldUsername); return; }

        // If the username changed, re-check uniqueness (excluding this same record).
        if (!c.getUsername().equalsIgnoreCase(oldUsername) && customerDAO.usernameExists(c.getUsername())) {
            selected.setUsername(oldUsername);
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }

        if (duplicateContact(c)) { selected.setUsername(oldUsername); return; }

        c.setPasswordHash(selected.getPasswordHash()); // don't touch password here — use Reset Password for that
        customerDAO.update(c);
        JOptionPane.showMessageDialog(this, "Customer updated.");
        clearForm();
        refreshTable();
    }

    private void deleteCustomer() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete customers")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\n" + selected.getUsername(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            customerDAO.delete(selected.getId());
            JOptionPane.showMessageDialog(this, "Customer deleted.");
            clearForm();
            refreshTable();
        } catch (RuntimeException ex) {
            ErrorHandler.show(this, ex);   // e.g. customer still has rentals/reservations (foreign key)
        }
    }

    private void resetPassword() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        String newPassword = PasswordPrompt.ask(this, selected.getUsername());   // JPasswordField, never shown as plain text
        if (newPassword == null || newPassword.isBlank()) return;
        if (!Validator.isValidPassword(newPassword)) {
            JOptionPane.showMessageDialog(this, "Password must be at least " + Validator.MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        selected.setPasswordHash(PasswordHasher.hash(newPassword));
        customerDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Password reset.");
    }

    /** Duplicate records: e-mail and driver's license must be unique (blank = not entered, so it is skipped). */
    private boolean duplicateContact(Customer c) {
        if (c.getEmail() != null && uniqueValues.customerEmailExists(c.getEmail(), c.getId())) {
            JOptionPane.showMessageDialog(this, "That email address is already used by another customer.");
            return true;
        }
        if (c.getLicenseNumber() != null && uniqueValues.customerLicenseExists(c.getLicenseNumber(), c.getId())) {
            JOptionPane.showMessageDialog(this, "That driver's license number is already registered to another customer.");
            return true;
        }
        return false;
    }

    /** A different temporary password for every new customer (a fixed one would be guessable by anyone). */
    private static String randomTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private Customer readForm(Customer c) {
        if (txtUsername.getText().isBlank() || txtFirstName.getText().isBlank() || txtLastName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Username, first name, and last name are required.");
            return null;
        }
        if (!Validator.isValidEmail(txtEmail.getText())) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address (e.g. name@example.com).",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!Validator.isValidPhone(txtPhone.getText())) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 11 digits.",
                    "Invalid Phone", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        c.setUsername(txtUsername.getText().trim());
        c.setFirstName(txtFirstName.getText().trim());
        c.setLastName(txtLastName.getText().trim());
        // Optional fields: save a blank box as NULL, not "". The database has UNIQUE email / license_number, and
        // two customers with an empty "" would be rejected as duplicates (NULLs never clash with each other).
        c.setEmail(blankToNull(txtEmail.getText()));
        c.setPhone(blankToNull(txtPhone.getText()));
        c.setLicenseNumber(blankToNull(txtLicense.getText()));
        c.setActive(chkActive.isSelected());
        return c;
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtUsername.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtLicense.setText("");
        chkActive.setSelected(true);
        table.clearSelection();
    }
}
