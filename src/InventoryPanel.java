import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

/** Admin/Employee screen: log stock receipts against supplies and flag low stock. */
public class InventoryPanel extends JPanel {

    private final InventoryStockDAO stockDAO = new InventoryStockDAO();
    private final SupplyDAO supplyDAO = new SupplyDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Supply", "Qty", "Unit Cost", "Received"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);

    private final JComboBox<Supply> cboSupply = new JComboBox<>();
    private final JTextField txtQty = new JTextField(8);
    private final JTextField txtCost = new JTextField(10);
    private final JLabel lblLowStock = new JLabel(" ");

    private InventoryStock selected;

    public InventoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(lblLowStock, BorderLayout.SOUTH);
        lblLowStock.setForeground(UITheme.RED_ACCENT);
        lblLowStock.setFont(UITheme.FONT_HEADER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        loadSupplies();
        refreshTable();
        refreshLowStockBanner();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Supply:", cboSupply);
        addField(form, gc, row++, "Quantity Received:", txtQty);
        addField(form, gc, row++, "Unit Cost:", txtCost);

        JButton btnAdd = UITheme.primaryButton("Receive Stock");
        JButton btnDelete = UITheme.primaryButton("Delete Entry");
        JButton btnClear = UITheme.primaryButton("Clear");
        btnAdd.addActionListener(e -> receiveStock());
        btnDelete.addActionListener(e -> deleteStock());
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnAdd);
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

    private void loadSupplies() {
        cboSupply.removeAllItems();
        for (Supply s : supplyDAO.findAll()) cboSupply.addItem(s);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (InventoryStock i : stockDAO.findAll()) {
            tableModel.addRow(new Object[]{
                i.id, i.supplyName + " (" + i.unit + ")", i.quantity, UITheme.peso(i.unitCost), i.dateReceived.toLocalDate()
            });
        }
    }

    private void refreshLowStockBanner() {
        StringBuilder sb = new StringBuilder();
        for (Supply s : supplyDAO.findAll()) {
            int onHand = supplyDAO.currentQuantity(s.id);
            if (onHand <= s.reorderLevel) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.name).append(" (").append(onHand).append(")");
            }
        }
        lblLowStock.setText(sb.length() == 0 ? "All supplies above reorder level." : "Low stock: " + sb);
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = stockDAO.findById(id);
        selectSupplyCombo(selected.supplyId);
        txtQty.setText(String.valueOf(selected.quantity));
        txtCost.setText(selected.unitCost.toString());
    }

    private void selectSupplyCombo(int supplyId) {
        for (int i = 0; i < cboSupply.getItemCount(); i++) {
            if (cboSupply.getItemAt(i).id == supplyId) { cboSupply.setSelectedIndex(i); return; }
        }
    }

    private void receiveStock() {
        InventoryStock i = readForm(new InventoryStock());
        if (i == null) return;
        stockDAO.insert(i);
        JOptionPane.showMessageDialog(this, "Stock received.");
        clearForm();
        refreshTable();
        refreshLowStockBanner();
    }

    private void deleteStock() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a stock entry first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this stock entry?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        stockDAO.delete(selected.id);
        clearForm();
        refreshTable();
        refreshLowStockBanner();
    }

    private InventoryStock readForm(InventoryStock i) {
        Supply sup = (Supply) cboSupply.getSelectedItem();
        if (sup == null) { JOptionPane.showMessageDialog(this, "Select a supply (add one under Supplies first)."); return null; }
        int qty;
        BigDecimal cost;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.");
            return null;
        }
        try {
            cost = new BigDecimal(txtCost.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Unit cost must be a number, e.g. 150.00");
            return null;
        }
        i.supplyId = sup.id;
        i.quantity = qty;
        i.unitCost = cost;
        return i;
    }

    private void clearForm() {
        selected = null;
        txtQty.setText("");
        txtCost.setText("");
        if (cboSupply.getItemCount() > 0) cboSupply.setSelectedIndex(0);
        table.clearSelection();
    }
}
