import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin/Employee screen: manage the catalog of supplies (parts, oil, cleaning supplies, etc.). */
public class SuppliesPanel extends JPanel {

    private final SupplyDAO supplyDAO = new SupplyDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Name", "Unit", "Supplier", "Reorder Level", "On Hand"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);

    private final JTextField txtName = new JTextField(15);
    private final JTextField txtUnit = new JTextField(10);
    private final JComboBox<Supplier> cboSupplier = new JComboBox<>();
    private final JTextField txtReorder = new JTextField(6);

    private Supply selected;

    public SuppliesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        cboSupplier.addItem(null); // "no supplier"
        for (Supplier s : supplierDAO.findAll()) cboSupplier.addItem(s);

        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Supply Name:", txtName);
        addField(form, gc, row++, "Unit (pcs, liters...):", txtUnit);
        addField(form, gc, row++, "Supplier:", cboSupplier);
        addField(form, gc, row++, "Reorder Level:", txtReorder);

        JButton btnAdd = UITheme.primaryButton("Add");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        btnAdd.addActionListener(e -> addSupply());
        btnUpdate.addActionListener(e -> updateSupply());
        btnDelete.addActionListener(e -> deleteSupply());
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
        for (Supply s : supplyDAO.findAll()) {
            int onHand = supplyDAO.currentQuantity(s.id);
            tableModel.addRow(new Object[]{
                s.id, s.name, s.unit, s.supplierName != null ? s.supplierName : "—", s.reorderLevel, onHand
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = supplyDAO.findById(id);

        txtName.setText(selected.name);
        txtUnit.setText(selected.unit);
        txtReorder.setText(String.valueOf(selected.reorderLevel));
        selectSupplierCombo(selected.supplierId);
    }

    private void selectSupplierCombo(Integer supplierId) {
        for (int i = 0; i < cboSupplier.getItemCount(); i++) {
            Supplier s = cboSupplier.getItemAt(i);
            if (s == null && supplierId == null) { cboSupplier.setSelectedIndex(i); return; }
            if (s != null && s.id == (supplierId == null ? -1 : supplierId)) { cboSupplier.setSelectedIndex(i); return; }
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
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a supply first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this supply?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        supplyDAO.delete(selected.id);
        clearForm();
        refreshTable();
    }

    private Supply readForm(Supply s) {
        if (txtName.getText().isBlank() || txtUnit.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Name and unit are required.");
            return null;
        }
        int reorder;
        try {
            reorder = Integer.parseInt(txtReorder.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Reorder level must be a whole number.");
            return null;
        }
        Supplier sup = (Supplier) cboSupplier.getSelectedItem();
        s.name = txtName.getText().trim();
        s.unit = txtUnit.getText().trim();
        s.supplierId = sup != null ? sup.id : null;
        s.reorderLevel = reorder;
        return s;
    }

    private void clearForm() {
        selected = null;
        txtName.setText("");
        txtUnit.setText("");
        txtReorder.setText("");
        cboSupplier.setSelectedIndex(0);
        table.clearSelection();
    }
}
