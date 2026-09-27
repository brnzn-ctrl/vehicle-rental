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

    private final JTextField txtUsername = new JTextField(15);
    private final JTextField txtFirstName = new JTextField(15);
    private final JTextField txtLastName = new JTextField(15);
    private final JTextField txtEmail = new JTextField(15);
    private final JTextField txtPhone = new JTextField(15);
    private final JTextField txtLicense = new JTextField(15);
    private final JCheckBox chkActive = new JCheckBox("Active", true);

    private Customer selected;

    public CustomerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

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
        addField(form, gc, row++, "Username:", txtUsername);
        addField(form, gc, row++, "First Name:", txtFirstName);
        addField(form, gc, row++, "Last Name:", txtLastName);
        addField(form, gc, row++, "Email:", txtEmail);
        addField(form, gc, row++, "Phone:", txtPhone);
        addField(form, gc, row++, "License #:", txtLicense);

        gc.gridx = 1; gc.gridy = row++;
        form.add(chkActive, gc);

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnResetPassword = new JButton("Reset Password");
        JButton btnClear = new JButton("Clear");
        btnAdd.addActionListener(e -> addCustomer());
        btnUpdate.addActionListener(e -> updateCustomer());
        btnResetPassword.addActionListener(e -> resetPassword());
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnResetPassword);
        buttons.add(btnClear);
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

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Customer c : customerDAO.findAll()) {
            tableModel.addRow(new Object[]{
                c.id, c.username, c.firstName + " " + c.lastName, c.email, c.phone, c.licenseNumber,
                c.active ? "Yes" : "No"
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = customerDAO.findById(id);

        txtUsername.setText(selected.username);
        txtFirstName.setText(selected.firstName);
        txtLastName.setText(selected.lastName);
        txtEmail.setText(selected.email);
        txtPhone.setText(selected.phone);
        txtLicense.setText(selected.licenseNumber);
        chkActive.setSelected(selected.active);
    }

    private void addCustomer() {
        Customer c = readForm(new Customer());
        if (c == null) return;

        if (customerDAO.usernameExists(c.username)) {
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }

        String tempPassword = "changeme123";
        c.passwordHash = PasswordUtil.hash(tempPassword);
        customerDAO.insert(c);
        JOptionPane.showMessageDialog(this,
            "Customer added. Temporary password: " + tempPassword + "\n(Tell them to change it after logging in.)");
        clearForm();
        refreshTable();
    }

    private void updateCustomer() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        Customer c = readForm(selected);
        if (c == null) return;

        // If the username changed, re-check uniqueness (excluding this same record).
        if (!c.username.equalsIgnoreCase(selected.username) && customerDAO.usernameExists(c.username)) {
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }

        c.passwordHash = selected.passwordHash; // don't touch password here — use Reset Password for that
        customerDAO.update(c);
        JOptionPane.showMessageDialog(this, "Customer updated.");
        clearForm();
        refreshTable();
    }

    private void resetPassword() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        String newPassword = JOptionPane.showInputDialog(this, "New password for " + selected.username + ":");
        if (newPassword == null || newPassword.isBlank()) return;
        selected.passwordHash = PasswordUtil.hash(newPassword);
        customerDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Password reset.");
    }

    private Customer readForm(Customer c) {
        if (txtUsername.getText().isBlank() || txtFirstName.getText().isBlank() || txtLastName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Username, first name, and last name are required.");
            return null;
        }
        c.username = txtUsername.getText().trim();
        c.firstName = txtFirstName.getText().trim();
        c.lastName = txtLastName.getText().trim();
        c.email = txtEmail.getText().trim();
        c.phone = txtPhone.getText().trim();
        c.licenseNumber = txtLicense.getText().trim();
        c.active = chkActive.isSelected();
        return c;
    }

    private void clearForm() {
        selected = null;
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
