package ui;

import util.Validator;

import util.AccessControl;
import util.ErrorHandler;

import util.UITheme;
import util.PasswordHasher;
import dao.StaffDAO;
import model.Staff;
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
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

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
        FormSupport.onDoubleClick(table, this::onRowSelected);   // MouseListener: double-click a row to load it into the form
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);

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
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Username:", txtUsername);
        addField(form, gc, row++, "First Name:", txtFirstName);
        addField(form, gc, row++, "Last Name:", txtLastName);
        addField(form, gc, row++, "Password (leave blank to keep):", txtPassword);
        addField(form, gc, row++, "Role:", cboRole);

        chkActive.setBackground(UITheme.BG_DARK);
        chkActive.setForeground(UITheme.TEXT_LIGHT);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        form.add(chkActive, gc);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addStaff));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateStaff));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteStaff));
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        buttons.add(btnRefresh);
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

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Staff s : staffDAO.search(keyword)) {
            tableModel.addRow(new Object[]{s.getId(), s.getUsername(), s.getFirstName() + " " + s.getLastName(), s.getRole(), s.isActive() ? "Yes" : "No"});
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = staffDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());

        txtUsername.setText(selected.getUsername());
        txtFirstName.setText(selected.getFirstName());
        txtLastName.setText(selected.getLastName());
        txtPassword.setText("");
        cboRole.setSelectedItem(selected.getRole());
        chkActive.setSelected(selected.isActive());
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
        if (!Validator.isValidPassword(pw)) {
            JOptionPane.showMessageDialog(this, "Password must be at least " + Validator.MIN_PASSWORD_LENGTH + " characters.");
            return;
        }

        Staff s = new Staff();
        s.setUsername(txtUsername.getText().trim());
        s.setPasswordHash(PasswordHasher.hash(pw));
        s.setFirstName(txtFirstName.getText().trim());
        s.setLastName(txtLastName.getText().trim());
        s.setRole((String) cboRole.getSelectedItem());
        s.setActive(chkActive.isSelected());
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
        String newUsername = txtUsername.getText().trim();
        if (newUsername.isEmpty()) { JOptionPane.showMessageDialog(this, "Username is required."); return; }
        // duplicate check on rename (compare with the OLD username before it is overwritten)
        if (!newUsername.equalsIgnoreCase(selected.getUsername()) && staffDAO.usernameExists(newUsername)) {
            JOptionPane.showMessageDialog(this, "That username is already taken.");
            return;
        }
        String newPw = new String(txtPassword.getPassword());
        if (!newPw.isBlank() && !Validator.isValidPassword(newPw)) {
            JOptionPane.showMessageDialog(this, "Password must be at least " + Validator.MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        if (selected.getId() == loggedInStaff.getId() && (!"admin".equals(cboRole.getSelectedItem()) || !chkActive.isSelected())) {
            JOptionPane.showMessageDialog(this, "You cannot remove your own admin role or deactivate your own account.");
            return;
        }
        selected.setUsername(newUsername);
        selected.setFirstName(txtFirstName.getText().trim());
        selected.setLastName(txtLastName.getText().trim());
        selected.setRole((String) cboRole.getSelectedItem());
        selected.setActive(chkActive.isSelected());
        String pw = new String(txtPassword.getPassword());
        if (!pw.isBlank()) selected.setPasswordHash(PasswordHasher.hash(pw));
        staffDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Staff account updated.");
        clearForm();
        refreshTable();
    }

    private void deleteStaff() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete staff accounts")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a staff account first."); return; }
        if (selected.getId() == loggedInStaff.getId()) { JOptionPane.showMessageDialog(this, "You cannot delete your own account."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?\n" + selected.getUsername(), "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        staffDAO.delete(selected.getId());
        JOptionPane.showMessageDialog(this, "Staff account deleted.");
        clearForm();
        refreshTable();
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtUsername.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        txtPassword.setText("");
        cboRole.setSelectedIndex(0);
        chkActive.setSelected(true);
        table.clearSelection();
    }
}
