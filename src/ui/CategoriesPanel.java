package ui;

import model.Staff;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;
import util.Validator;
import dao.VehicleCategoryDAO;
import model.VehicleCategory;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin screen: manage vehicle categories (Create / Read / Update / Delete + search). */
public class CategoriesPanel extends JPanel {
    private final dao.UniqueValueChecker uniqueChecker = new dao.UniqueValueChecker();

    private final VehicleCategoryDAO categoryDAO = new VehicleCategoryDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Category Name"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected
    private final JTextField txtName = new JTextField(20);

    private VehicleCategory selected;

    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use CategoriesPanel(Staff). */
    public CategoriesPanel() { this(null); }

    public CategoriesPanel(Staff loggedInStaff) {
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

        gc.gridx = 0; gc.gridy = 0;
        JLabel lid = new JLabel("ID:");
        lid.setForeground(UITheme.TEXT_LIGHT);
        form.add(lid, gc);
        gc.gridx = 1;
        form.add(txtId, gc);

        gc.gridx = 0; gc.gridy = 1;
        JLabel l = new JLabel("Category Name:");
        l.setForeground(UITheme.TEXT_LIGHT);
        form.add(l, gc);
        gc.gridx = 1;
        form.add(txtName, gc);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addCategory));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateCategory));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteCategory));
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        buttons.add(btnRefresh);
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        form.add(buttons, gc);
        return form;
    }

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (VehicleCategory c : categoryDAO.search(keyword)) tableModel.addRow(new Object[]{c.getId(), c.getName()});
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = categoryDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());
        txtName.setText(selected.getName());
    }

    private void addCategory() {
        String name = readName(0);
        if (name == null) return;
        categoryDAO.insert(new VehicleCategory(0, name));
        JOptionPane.showMessageDialog(this, "Category added.");
        clearForm();
        refreshTable();
    }

    private void updateCategory() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a category first."); return; }
        String name = readName(selected.getId());
        if (name == null) return;
        selected.setName(name);
        categoryDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Category updated.");
        clearForm();
        refreshTable();
    }

    private void deleteCategory() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete vehicle categories")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a category first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\n" + selected.getName(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        categoryDAO.delete(selected.getId());   // blocked by the foreign key if vehicles still use it
        JOptionPane.showMessageDialog(this, "Category deleted.");
        clearForm();
        refreshTable();
    }

    private String readName(int ownId) {
        String name = txtName.getText().trim();
        if (Validator.isBlank(name)) {
            JOptionPane.showMessageDialog(this, "Category name is required.");
            return null;
        }
        if (name.length() > 50) {
            JOptionPane.showMessageDialog(this, "Category name is too long (50 characters max).");
            return null;
        }
        if (uniqueChecker.categoryNameExists(name, ownId)) {      // SELECT COUNT(*) ... WHERE
            JOptionPane.showMessageDialog(this, "That category already exists.");
            return null;
        }
        return name;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtName.setText("");
        table.clearSelection();
    }
}
