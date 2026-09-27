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
        add(new JScrollPane(table), BorderLayout.CENTER);

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
        addField(form, gc, row++, "Category:", txtCategory);
        addField(form, gc, row++, "Description:", txtDescription);
        addField(form, gc, row++, "Amount:", txtAmount);
        addField(form, gc, row++, "Date (YYYY-MM-DD):", txtDate);

        JButton btnAdd = UITheme.primaryButton("Add");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        btnAdd.addActionListener(e -> addExpense());
        btnUpdate.addActionListener(e -> updateExpense());
        btnDelete.addActionListener(e -> deleteExpense());
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
        for (Expense e : expenseDAO.findAll()) {
            tableModel.addRow(new Object[]{
                e.id, e.category, e.description, UITheme.peso(e.amount), e.expenseDate, e.recordedByName
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        selected = expenseDAO.findById(id);

        txtCategory.setText(selected.category);
        txtDescription.setText(selected.description);
        txtAmount.setText(selected.amount.toString());
        txtDate.setText(selected.expenseDate.toString());
    }

    private void addExpense() {
        Expense e = readForm(new Expense());
        if (e == null) return;
        e.recordedBy = loggedInStaff.id;
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
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an expense first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this expense?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        expenseDAO.delete(selected.id);
        clearForm();
        refreshTable();
    }

    private Expense readForm(Expense e) {
        if (txtCategory.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Category is required.");
            return null;
        }
        BigDecimal amount;
        LocalDate date;
        try {
            amount = new BigDecimal(txtAmount.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number, e.g. 500.00");
            return null;
        }
        try {
            date = LocalDate.parse(txtDate.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format.");
            return null;
        }
        e.category = txtCategory.getText().trim();
        e.description = txtDescription.getText().trim();
        e.amount = amount;
        e.expenseDate = date;
        return e;
    }

    private void clearForm() {
        selected = null;
        txtCategory.setText("");
        txtDescription.setText("");
        txtAmount.setText("");
        txtDate.setText(LocalDate.now().toString());
        table.clearSelection();
    }
}
