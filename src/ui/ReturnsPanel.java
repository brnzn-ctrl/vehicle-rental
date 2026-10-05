package ui;

import dao.ReturnDAO;
import model.ReturnRecord;
import model.Staff;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Admin/Employee screen: view, search, update and delete return records (JTable, JOIN to rentals/customers/vehicles/staff).
 * New returns are created by "Return & Pay" on the Rentals screen, because that one transaction also writes the bill,
 * the payment row and frees the vehicle. Fees are therefore read-only here; only the condition notes can be edited.
 */
public class ReturnsPanel extends JPanel {

    private final ReturnDAO returnDAO = new ReturnDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"Return ID", "Rental ID", "Customer", "Vehicle", "Returned", "Late days",
                                           "Late fee", "Damage fee", "Condition notes", "Received by"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();
    private final JTextField txtRental = FormSupport.idField();
    private final JTextField txtNotes = new JTextField(30);

    private int selectedId = 0;
    private String keyword = "";

    public ReturnsPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
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
        addField(form, gc, row++, "Return ID:", txtId);
        addField(form, gc, row++, "Rental ID:", txtRental);
        addField(form, gc, row++, "Condition notes:", txtNotes);

        JLabel note = new JLabel("To record a new return, use Rentals > Return & Pay (it also creates the payment).");
        note.setForeground(UITheme.TEXT_LIGHT);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        form.add(note, gc);

        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateReturn));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteReturn));
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> refreshTable());

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
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

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (ReturnRecord r : returnDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                r.getId(), r.getRentalId(), r.getCustomerName(), r.getVehicleName(),
                r.getReturnDate() == null ? "" : r.getReturnDate().toLocalDate(),
                r.getLateDays(), UITheme.peso(r.getLateFee()), UITheme.peso(r.getDamageFee()),
                r.getConditionNotes() == null ? "" : r.getConditionNotes(), r.getReceivedByName()
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int m = table.convertRowIndexToModel(row);
        selectedId = (int) tableModel.getValueAt(m, 0);
        FormSupport.showId(txtId, selectedId);
        FormSupport.showId(txtRental, (int) tableModel.getValueAt(m, 1));
        Object notes = tableModel.getValueAt(m, 8);
        txtNotes.setText(notes == null ? "" : notes.toString());
    }

    private void updateReturn() {
        if (selectedId == 0) { JOptionPane.showMessageDialog(this, "Select a return record first."); return; }
        String notes = txtNotes.getText().trim();
        if (notes.length() > 255) {
            JOptionPane.showMessageDialog(this, "Condition notes can be at most 255 characters.");
            return;
        }
        ReturnRecord rec = returnDAO.findById(selectedId);
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "That return record no longer exists. The list will be refreshed.");
            clearForm();
            refreshTable();
            return;
        }
        rec.setConditionNotes(notes.isEmpty() ? null : notes);
        returnDAO.update(rec);
        JOptionPane.showMessageDialog(this, "Return record updated.");
        clearForm();
        refreshTable();
    }

    private void deleteReturn() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete return records")) return;
        if (selectedId == 0) { JOptionPane.showMessageDialog(this, "Select a return record first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\nReturn #" + selectedId,
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        returnDAO.delete(selectedId);
        JOptionPane.showMessageDialog(this, "Return record deleted.");
        clearForm();
        refreshTable();
    }

    private void clearForm() {
        selectedId = 0;
        txtId.setText("");
        txtRental.setText("");
        txtNotes.setText("");
        table.clearSelection();
    }
}
