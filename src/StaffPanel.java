import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin-only screen: manage staff/employee accounts. */
public class StaffPanel extends JPanel {

    private final StaffDAO staffDAO = new StaffDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Username", "Name", "Role", "Active"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);

    private final JTextField txtUsername = new JTextField(15);
    private final JTextField txtFirstName = new JTextField(15);
    private final JTextField txtLastName = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JComboBox<String> cboRole = new JComboBox<>(new String[]{"employee", "admin"});
    private final JCheckBox chkActive = new JCheckBox("Active", true);

    private Staff selected;

    public StaffPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Username:", txtUsername);
        addField(form, gc, row++, "First Name:", txtFirstName);
        addField(form, gc, row++, "Last Name:", txtLastName);
        addField(form, gc, row++, "Password (leave blank to keep):", txtPassword);
        addField(form, gc, row++, "Role:", cboRole);

        chkActive.setBackground(UITheme.BG_DARK);
        chkActive.setForeground(UITheme.TEXT_LIGHT);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        form.add(chkActive, gc);

        JButton btnAdd = UITheme.primaryButton("Add");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        btnAdd.addActionListener(e -> addStaff());
        btnUpdate.addActionListener(e -> updateStaff());
        btnDelete.addActionListener(e -> deleteStaff());
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        form.add(buttons, gc);

        return form;
    }

    private void addField(JPanel form, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        JLabel l = new JLabel(label);
        l.setForeground(UITheme.TEXT_LIGHT);
        form.add(l, gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Staff s : staffDAO.findAll()) {
            tableModel.addRow(new Object[]{s.id, s.username, s.firstName + " " + s.lastName, s.role, s.active ? "Yes" : "No"});
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = staffDAO.findById(id);

        txtUsername.setText(selected.username);
        txtFirstName.setText(selected.firstName);
        txtLastName.setText(selected.lastName);
        txtPassword.setText("");
        cboRole.setSelectedItem(selected.role);
        chkActive.setSelected(selected.active);
    }

    private void addStaff() {
        if (txtUsername.getText().isBlank() || txtFirstName.getText().isBlank() || txtLastName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Username, first name and last name are required.");
            return;
        }
        if (staffDAO.usernameExists(txtUsername.getText().trim())) {
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }
        String pw = new String(txtPassword.getPassword());
        if (pw.isBlank()) { JOptionPane.showMessageDialog(this, "Set a password for the new account."); return; }

        Staff s = new Staff();
        s.username = txtUsername.getText().trim();
        s.passwordHash = PasswordUtil.hash(pw);
        s.firstName = txtFirstName.getText().trim();
        s.lastName = txtLastName.getText().trim();
        s.role = (String) cboRole.getSelectedItem();
        s.active = chkActive.isSelected();
        staffDAO.insert(s);
        JOptionPane.showMessageDialog(this, "Staff account added.");
        clearForm();
        refreshTable();
    }

    private void updateStaff() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a staff account first."); return; }
        if (txtFirstName.getText().isBlank() || txtLastName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "First name and last name are required.");
            return;
        }
        selected.username = txtUsername.getText().trim();
        selected.firstName = txtFirstName.getText().trim();
        selected.lastName = txtLastName.getText().trim();
        selected.role = (String) cboRole.getSelectedItem();
        selected.active = chkActive.isSelected();
        String pw = new String(txtPassword.getPassword());
        if (!pw.isBlank()) selected.passwordHash = PasswordUtil.hash(pw);
        staffDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Staff account updated.");
        clearForm();
        refreshTable();
    }

    private void deleteStaff() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a staff account first."); return; }
        if (selected.id == loggedInStaff.id) { JOptionPane.showMessageDialog(this, "You cannot delete your own account."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this staff account?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        staffDAO.delete(selected.id);
        clearForm();
        refreshTable();
    }

    private void clearForm() {
        selected = null;
        txtUsername.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        txtPassword.setText("");
        cboRole.setSelectedIndex(0);
        chkActive.setSelected(true);
        table.clearSelection();
    }
}
