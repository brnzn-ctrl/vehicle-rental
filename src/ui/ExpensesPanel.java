package ui;

import util.Validator;

import util.AccessControl;
import util.ErrorHandler;

import util.UITheme;
import dao.ExpenseDAO;
import model.Staff;
import model.Expense;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin/Employee screen: log shop expenses (maintenance, fuel, salary, utilities, etc.). */
public class ExpensesPanel extends JPanel {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Category", "Description", "Amount", "Date", "Recorded By"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JTextField txtCategory = new JTextField(15);
    private final JTextField txtDescription = new JTextField(20);
    private final JTextField txtAmount = new JTextField(10);
    private final JTextField txtDate = new JTextField(10);

    private Expense selected;

    public ExpensesPanel(Staff loggedInStaff) {
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

        txtDate.setText(LocalDate.now().toString());
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
        addField(form, gc, row++, "Category:", txtCategory);
        addField(form, gc, row++, "Description:", txtDescription);
        addField(form, gc, row++, "Amount:", txtAmount);
        addField(form, gc, row++, "Date (YYYY-MM-DD):", txtDate);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addExpense));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateExpense));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteExpense));
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
        for (Expense e : expenseDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                e.getId(), e.getCategory(), e.getDescription(), UITheme.peso(e.getAmount()), e.getExpenseDate(), e.getRecordedByName()
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = expenseDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());

        txtCategory.setText(selected.getCategory());
        txtDescription.setText(selected.getDescription());
        txtAmount.setText(selected.getAmount().toString());
        txtDate.setText(selected.getExpenseDate().toString());
    }

    private void addExpense() {
        Expense e = readForm(new Expense());
        if (e == null) return;
        e.setRecordedBy(loggedInStaff.getId());
        expenseDAO.insert(e);
        JOptionPane.showMessageDialog(this, "Expense recorded.");
        clearForm();
        refreshTable();
    }

    private void updateExpense() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an expense first."); return; }
        Expense e = readForm(selected);
        if (e == null) return;
        expenseDAO.update(e);
        JOptionPane.showMessageDialog(this, "Expense updated.");
        clearForm();
        refreshTable();
    }

    private void deleteExpense() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete expenses")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an expense first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        expenseDAO.delete(selected.getId());
        JOptionPane.showMessageDialog(this, "Expense deleted.");
        clearForm();
        refreshTable();
    }

    private Expense readForm(Expense e) {
        if (txtCategory.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Category is required.");
            return null;
        }
        BigDecimal amount = Validator.parsePositive(txtAmount.getText());
        if (amount == null) {
            JOptionPane.showMessageDialog(this, "Amount must be a number greater than 0, e.g. 500.00");
            return null;
        }
        LocalDate date = Validator.parseDate(txtDate.getText());
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Date must be a valid date in YYYY-MM-DD format.");
            return null;
        }
        if (date.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Expense date cannot be in the future.");
            return null;
        }
        e.setCategory(txtCategory.getText().trim());
        e.setDescription(txtDescription.getText().trim());
        e.setAmount(amount);
        e.setExpenseDate(date);
        return e;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtCategory.setText("");
        txtDescription.setText("");
        txtAmount.setText("");
        txtDate.setText(LocalDate.now().toString());
        table.clearSelection();
    }
}
