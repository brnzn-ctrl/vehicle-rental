package ui;

import util.Validator;

import model.Staff;
import util.AccessControl;
import util.ErrorHandler;

import util.UITheme;
import dao.SupplyDAO;
import dao.SupplierDAO;
import model.Supplier;
import model.Supply;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin/Employee screen: manage the catalog of supplies (parts, oil, cleaning supplies, etc.). */
public class SuppliesPanel extends JPanel {
    private final dao.UniqueValueChecker uniqueChecker = new dao.UniqueValueChecker();

    private final SupplyDAO supplyDAO = new SupplyDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Name", "Unit", "Supplier", "Reorder Threshold", "On Hand", "Stock Level"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JTextField txtName = new JTextField(15);
    private final JTextField txtUnit = new JTextField(10);
    private final JComboBox<Supplier> cboSupplier = new JComboBox<>();
    private final JTextField txtReorderThreshold = new JTextField(6);

    private Supply selected;

    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use SuppliesPanel(Staff). */
    public SuppliesPanel() { this(null); }

    public SuppliesPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        FormSupport.onDoubleClick(table, this::onRowSelected);   // MouseListener: double-click a row to load it into the form
        add(SearchBar.wrap(table, this::runSearch, () -> { reloadSuppliers(); refreshTable(); }), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        reloadSuppliers();
        refreshTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { reloadSuppliers(); refreshTable(); }
        });
    }

    /** (Re)loads the supplier drop-down, so suppliers added on the Suppliers screen show up here. */
    private void reloadSuppliers() {
        Supplier keep = (Supplier) cboSupplier.getSelectedItem();
        cboSupplier.removeAllItems();
        cboSupplier.addItem(null); // "no supplier"
        for (Supplier sup : supplierDAO.findAll()) cboSupplier.addItem(sup);
        if (keep != null) {
            for (int i = 0; i < cboSupplier.getItemCount(); i++) {
                Supplier x = cboSupplier.getItemAt(i);
                if (x != null && x.getId() == keep.getId()) { cboSupplier.setSelectedIndex(i); break; }
            }
        }
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Supply Name:", txtName);
        addField(form, gc, row++, "Unit (pcs, liters...):", txtUnit);
        addField(form, gc, row++, "Supplier:", cboSupplier);
        addField(form, gc, row++, "Reorder Threshold:", txtReorderThreshold);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addSupply));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateSupply));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteSupply));
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

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Supply s : supplyDAO.search(keyword)) {
            int onHand = supplyDAO.currentQuantity(s.getId());
            tableModel.addRow(new Object[]{
                s.getId(), s.getName(), s.getUnit(), s.getSupplierName() != null ? s.getSupplierName() : "—",
                s.getReorderThreshold(), onHand, displayStockLevel(s.getStockLevel())
            });
        }
    }

    private String displayStockLevel(String stockLevel) {
        if ("critical stock".equals(stockLevel)) return "Critical Stock";
        if ("low stock".equals(stockLevel)) return "Low Stock";
        return "Sufficient";
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = supplyDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());

        txtName.setText(selected.getName());
        txtUnit.setText(selected.getUnit());
        txtReorderThreshold.setText(String.valueOf(selected.getReorderThreshold()));
        selectSupplierCombo(selected.getSupplierId());
    }

    private void selectSupplierCombo(Integer supplierId) {
        for (int i = 0; i < cboSupplier.getItemCount(); i++) {
            Supplier s = cboSupplier.getItemAt(i);
            if (s == null && supplierId == null) { cboSupplier.setSelectedIndex(i); return; }
            if (s != null && s.getId() == (supplierId == null ? -1 : supplierId)) { cboSupplier.setSelectedIndex(i); return; }
        }
    }

    private void addSupply() {
        Supply s = readForm(new Supply());
        if (s == null) return;
        supplyDAO.insert(s);
        JOptionPane.showMessageDialog(this, "Supply added.");
        clearForm();
        refreshTable();
    }

    private void updateSupply() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a supply first."); return; }
        Supply s = readForm(selected);
        if (s == null) return;
        supplyDAO.update(s);
        JOptionPane.showMessageDialog(this, "Supply updated.");
        clearForm();
        refreshTable();
    }

    private void deleteSupply() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete supplies")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a supply first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?\n" + selected.getName(), "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        supplyDAO.delete(selected.getId());
        JOptionPane.showMessageDialog(this, "Supply deleted.");
        clearForm();
        refreshTable();
    }

    private Supply readForm(Supply s) {
        if (txtName.getText().isBlank() || txtUnit.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Name and unit are required.");
            return null;
        }
        Integer reorderThreshold = Validator.parseNonNegativeInt(txtReorderThreshold.getText());
        if (reorderThreshold == null) {
            JOptionPane.showMessageDialog(this, "Reorder threshold must be a whole number (0 or more).");
            return null;
        }
        if (uniqueChecker.supplyNameExists(txtName.getText().trim(), s.getId())) {   // s.getId() is 0 for a new supply
            JOptionPane.showMessageDialog(this, "A supply with that name already exists.");
            return null;
        }
        Supplier sup = (Supplier) cboSupplier.getSelectedItem();
        s.setName(txtName.getText().trim());
        s.setUnit(txtUnit.getText().trim());
        s.setSupplierId(sup != null ? sup.getId() : null);
        s.setReorderThreshold(reorderThreshold);
        return s;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtName.setText("");
        txtUnit.setText("");
        txtReorderThreshold.setText("");
        cboSupplier.setSelectedIndex(0);
        table.clearSelection();
    }
}
