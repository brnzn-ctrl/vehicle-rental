package ui;

import model.Staff;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;
import util.Validator;
import dao.SupplierDAO;
import model.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin/Employee screen: manage suppliers (Create / Read / Update / Delete + search). */
public class SuppliersPanel extends JPanel {
    private final dao.UniqueValueChecker uniqueChecker = new dao.UniqueValueChecker();

    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Supplier Name", "Contact Number", "Address"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JTextField txtName = new JTextField(20);
    private final JTextField txtContact = new JTextField(12);
    private final JTextField txtAddress = new JTextField(25);

    private Supplier selected;

    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use SuppliersPanel(Staff). */
    public SuppliersPanel() { this(null); }

    public SuppliersPanel(Staff loggedInStaff) {
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
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { refreshTable(); }
        });
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Supplier Name:", txtName);
        addField(form, gc, row++, "Contact Number (11 digits):", txtContact);
        addField(form, gc, row++, "Address:", txtAddress);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addSupplier));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateSupplier));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteSupplier));
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
        for (Supplier s : supplierDAO.search(keyword)) {
            tableModel.addRow(new Object[]{s.getId(), s.getName(), s.getContactNumber() == null ? "" : s.getContactNumber(),
                s.getAddress() == null ? "" : s.getAddress()});
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = supplierDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());
        txtName.setText(selected.getName());
        txtContact.setText(selected.getContactNumber());
        txtAddress.setText(selected.getAddress());
    }

    private void addSupplier() {
        Supplier s = readForm(new Supplier(), 0);
        if (s == null) return;
        supplierDAO.insert(s);
        JOptionPane.showMessageDialog(this, "Supplier added.");
        clearForm();
        refreshTable();
    }

    private void updateSupplier() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a supplier first."); return; }
        Supplier s = readForm(selected, selected.getId());
        if (s == null) return;
        supplierDAO.update(s);
        JOptionPane.showMessageDialog(this, "Supplier updated.");
        clearForm();
        refreshTable();
    }

    private void deleteSupplier() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete suppliers")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a supplier first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\n" + selected.getName(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        supplierDAO.delete(selected.getId());   // blocked by the foreign key if a supply still uses this supplier
        JOptionPane.showMessageDialog(this, "Supplier deleted.");
        clearForm();
        refreshTable();
    }

    /** Validates the form and copies the values into s. Returns null (after a message) if invalid. */
    private Supplier readForm(Supplier s, int ownId) {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier name is required.");
            return null;
        }
        if (!Validator.isValidPhone(txtContact.getText())) {
            JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits.");
            return null;
        }
        if (uniqueChecker.supplierNameExists(name, ownId)) {      // duplicate check: SELECT COUNT(*) ... WHERE
            JOptionPane.showMessageDialog(this, "A supplier with that name already exists.");
            return null;
        }
        s.setName(name);
        s.setContactNumber(Validator.isBlank(txtContact.getText()) ? null : txtContact.getText().trim());
        s.setAddress(Validator.isBlank(txtAddress.getText()) ? null : txtAddress.getText().trim());
        return s;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtName.setText("");
        txtContact.setText("");
        txtAddress.setText("");
        table.clearSelection();
    }
}
