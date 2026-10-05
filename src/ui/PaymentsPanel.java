package ui;

import dao.PaymentDAO;
import dao.RentalDAO;
import model.Payment;
import model.Rental;
import model.Staff;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;
import util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Admin/Employee screen: view, search, add, update and delete payments (JTable, JOIN to rentals/customers/vehicles/staff). */
public class PaymentsPanel extends JPanel {

    private static final String[] METHODS = {"cash", "card", "gcash", "bank_transfer"};
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");   // DECIMAL(10,2)

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final RentalDAO rentalDAO = new RentalDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"Payment ID", "Rental ID", "Customer", "Vehicle", "Amount", "Method", "Date", "Processed by"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();

    private static final class RentalItem {
        final int id; final String label;
        RentalItem(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
    private final JComboBox<RentalItem> cboRental = new JComboBox<>();
    private final JTextField txtAmount = new JTextField(10);
    private final JComboBox<String> cboMethod = new JComboBox<>(METHODS);
    private final JLabel lblTotal = new JLabel(" ");

    private int selectedId = 0;
    private String keyword = "";

    public PaymentsPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);
        lblTotal.setForeground(UITheme.TEXT_LIGHT);
        add(lblTotal, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        loadRentals();
        refreshTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadRentals(); refreshTable(); }
        });
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Payment ID:", txtId);
        addField(form, gc, row++, "Rental (returned):", cboRental);
        addField(form, gc, row++, "Amount:", txtAmount);
        addField(form, gc, row++, "Method:", cboMethod);

        JButton btnSave = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnSave.addActionListener(e -> ErrorHandler.run(this, this::addPayment));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updatePayment));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deletePayment));
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> { loadRentals(); refreshTable(); });

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnSave);
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

    private void loadRentals() {
        cboRental.removeAllItems();
        for (Rental r : rentalDAO.findReturnedWithoutPayment()) {
            cboRental.addItem(new RentalItem(r.getId(),
                "Rental #" + r.getId() + " - " + r.getCustomerName() + " - " + r.getVehicleName()));
        }
    }

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        List<Payment> rows = paymentDAO.search(keyword);
        for (Payment p : rows) {
            total = total.add(p.getAmount());
            tableModel.addRow(new Object[]{
                p.getId(), p.getRentalId(), p.getCustomerName(), p.getVehicleName(), UITheme.peso(p.getAmount()),
                p.getPaymentMethod(), p.getPaymentDate() == null ? "" : p.getPaymentDate().toString().replace('T', ' ').substring(0, 16),
                p.getProcessedByName()
            });
        }
        lblTotal.setText("Payments shown: " + rows.size() + "    Total: " + UITheme.peso(total));
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        selectedId = (int) tableModel.getValueAt(m, 0);
        FormSupport.showId(txtId, selectedId);
        int rentalId = (int) tableModel.getValueAt(m, 1);
        for (int i = 0; i < cboRental.getItemCount(); i++)
            if (cboRental.getItemAt(i).id == rentalId) { cboRental.setSelectedIndex(i); break; }
        // the table shows a formatted peso string, so read the exact amount from the database
        Payment p = paymentDAO.findById(selectedId);
        if (p != null) {
            txtAmount.setText(p.getAmount().toPlainString());
            cboMethod.setSelectedItem(p.getPaymentMethod());
        }
    }

    /** Returns the validated amount, or null after showing a message. */
    private BigDecimal readAmount() {
        BigDecimal amount = Validator.parseNonNegative(txtAmount.getText());
        if (amount == null) {
            JOptionPane.showMessageDialog(this, "Amount must be a number (0 or more).");
            return null;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            JOptionPane.showMessageDialog(this, "Amount is too large.");
            return null;
        }
        return amount;
    }

    private void addPayment() {
        RentalItem rental = (RentalItem) cboRental.getSelectedItem();
        if (rental == null) { JOptionPane.showMessageDialog(this, "Every returned rental already has a payment. Return & Pay creates it automatically."); return; }
        BigDecimal amount = readAmount();
        if (amount == null) return;
        if (paymentDAO.existsForRental(rental.id)) {
            JOptionPane.showMessageDialog(this, "This rental already has a payment. Select it in the table and use Update instead.");
            return;
        }
        Payment pay = new Payment();
        pay.setRentalId(rental.id);
        pay.setAmount(amount);
        pay.setPaymentMethod((String) cboMethod.getSelectedItem());
        pay.setProcessedBy(loggedInStaff.getId());
        paymentDAO.insert(pay);
        JOptionPane.showMessageDialog(this, "Payment saved.");
        clearForm();
        refreshTable();
    }

    private void updatePayment() {
        if (selectedId == 0) { JOptionPane.showMessageDialog(this, "Select a payment first."); return; }
        BigDecimal amount = readAmount();
        if (amount == null) return;
        Payment pay = paymentDAO.findById(selectedId);
        if (pay == null) {
            JOptionPane.showMessageDialog(this, "That payment no longer exists. The list will be refreshed.");
            clearForm();
            refreshTable();
            return;
        }
        pay.setAmount(amount);
        pay.setPaymentMethod((String) cboMethod.getSelectedItem());
        paymentDAO.update(pay);
        JOptionPane.showMessageDialog(this, "Payment updated.");
        clearForm();
        refreshTable();
    }

    private void deletePayment() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete payments")) return;
        if (selectedId == 0) { JOptionPane.showMessageDialog(this, "Select a payment first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\nPayment #" + selectedId,
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        paymentDAO.delete(selectedId);
        JOptionPane.showMessageDialog(this, "Payment deleted.");
        clearForm();
        refreshTable();
    }

    private void clearForm() {
        selectedId = 0;
        txtId.setText("");
        txtAmount.setText("");
        cboMethod.setSelectedIndex(0);
        if (cboRental.getItemCount() > 0) cboRental.setSelectedIndex(0);
        table.clearSelection();
    }
}
