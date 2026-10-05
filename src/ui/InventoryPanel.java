package ui;

import util.Validator;

import model.Staff;
import util.AccessControl;
import util.ErrorHandler;

import util.UITheme;
import dao.SupplyDAO;
import dao.InventoryStockDAO;
import model.Supply;
import model.InventoryStock;
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
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JComboBox<Supply> cboSupply = new JComboBox<>();
    private final JTextField txtQty = new JTextField(8);
    private final JTextField txtCost = new JTextField(10);
    private final JLabel lblLowStock = new JLabel(" ");

    private InventoryStock selected;

    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use InventoryPanel(Staff). */
    public InventoryPanel() { this(null); }

    public InventoryPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        FormSupport.onDoubleClick(table, this::onRowSelected);   // MouseListener: double-click a row to load it into the form
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);
        add(lblLowStock, BorderLayout.SOUTH);
        lblLowStock.setForeground(UITheme.RED_ACCENT);
        lblLowStock.setFont(UITheme.FONT_HEADER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadSupplies(); refreshTable(); refreshLowStockBanner(); }
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
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Supply:", cboSupply);
        addField(form, gc, row++, "Quantity Received:", txtQty);
        addField(form, gc, row++, "Unit Cost:", txtCost);

        JButton btnAdd = UITheme.primaryButton("Save (Receive Stock)");
        JButton btnUpdate = UITheme.primaryButton("Update Entry");
        JButton btnDelete = UITheme.primaryButton("Delete Entry");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> { loadSupplies(); refreshTable(); refreshLowStockBanner(); }));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::receiveStock));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateStock));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteStock));
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

    private void loadSupplies() {
        cboSupply.removeAllItems();
        for (Supply s : supplyDAO.findAll()) cboSupply.addItem(s);
    }

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (InventoryStock i : stockDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                i.getId(), i.getSupplyName() + " (" + i.getUnit() + ")", i.getQuantity(), UITheme.peso(i.getUnitCost()), i.getDateReceived().toLocalDate()
            });
        }
    }

    private void refreshLowStockBanner() {
        StringBuilder sb = new StringBuilder();
        for (Supply s : supplyDAO.findAll()) {
            int onHand = supplyDAO.currentQuantity(s.getId());
            if (onHand <= s.getReorderLevel()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.getName()).append(" (").append(onHand).append(")");
            }
        }
        lblLowStock.setText(sb.length() == 0 ? "All supplies above reorder level." : "Low stock: " + sb);
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = stockDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());
        selectSupplyCombo(selected.getSupplyId());
        txtQty.setText(String.valueOf(selected.getQuantity()));
        txtCost.setText(selected.getUnitCost().toString());
    }

    private void selectSupplyCombo(int supplyId) {
        for (int i = 0; i < cboSupply.getItemCount(); i++) {
            if (cboSupply.getItemAt(i).getId() == supplyId) { cboSupply.setSelectedIndex(i); return; }
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

    private void updateStock() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a stock entry first."); return; }
        InventoryStock i = readForm(selected);
        if (i == null) return;
        stockDAO.update(i);
        JOptionPane.showMessageDialog(this, "Stock entry updated.");
        clearForm();
        refreshTable();
        refreshLowStockBanner();
    }

    private void deleteStock() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete stock entries")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a stock entry first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        stockDAO.delete(selected.getId());
        JOptionPane.showMessageDialog(this, "Stock entry deleted.");
        clearForm();
        refreshTable();
        refreshLowStockBanner();
    }

    private InventoryStock readForm(InventoryStock i) {
        Supply sup = (Supply) cboSupply.getSelectedItem();
        if (sup == null) { JOptionPane.showMessageDialog(this, "Select a supply (add one under Supplies first)."); return null; }
        Integer qty = Validator.parsePositiveInt(txtQty.getText());
        if (qty == null) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number greater than 0.");
            return null;
        }
        BigDecimal cost = Validator.parseNonNegative(txtCost.getText());
        if (cost == null) {
            JOptionPane.showMessageDialog(this, "Unit cost must be a number (0 or more), e.g. 150.00");
            return null;
        }
        i.setSupplyId(sup.getId());
        i.setQuantity(qty);
        i.setUnitCost(cost);
        return i;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtQty.setText("");
        txtCost.setText("");
        if (cboSupply.getItemCount() > 0) cboSupply.setSelectedIndex(0);
        table.clearSelection();
    }
}
