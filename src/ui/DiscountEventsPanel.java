package ui;

import model.Staff;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;
import util.Validator;
import dao.DiscountEventDAO;
import model.DiscountEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin screen: manage holiday discounts used by reservations and rentals (CRUD + search). */
public class DiscountEventsPanel extends JPanel {
    private final dao.UniqueValueChecker uniqueChecker = new dao.UniqueValueChecker();

    private final DiscountEventDAO discountDAO = new DiscountEventDAO();

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Event", "Start Date", "End Date", "Discount %", "Active"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only, filled when a row is selected

    private final JTextField txtName = new JTextField(20);
    private final JTextField txtStart = new JTextField(10);
    private final JTextField txtEnd = new JTextField(10);
    private final JTextField txtPercent = new JTextField(6);
    private final JCheckBox chkActive = new JCheckBox("Active", true);

    private DiscountEvent selected;

    private final Staff loggedInStaff;   // used for the admin-only Delete

    /** Without a logged-in staff member nobody counts as admin, so Delete stays locked. Use DiscountEventsPanel(Staff). */
    public DiscountEventsPanel() { this(null); }

    public DiscountEventsPanel(Staff loggedInStaff) {
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

        int row = 0;
        addField(form, gc, row++, "ID:", txtId);
        addField(form, gc, row++, "Event Name:", txtName);
        addField(form, gc, row++, "Start Date (YYYY-MM-DD):", txtStart);
        addField(form, gc, row++, "End Date (YYYY-MM-DD):", txtEnd);
        addField(form, gc, row++, "Discount Percent (1-100):", txtPercent);

        chkActive.setBackground(UITheme.BG_DARK);
        chkActive.setForeground(UITheme.TEXT_LIGHT);
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        form.add(chkActive, gc);

        JButton btnAdd = UITheme.primaryButton("Save");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnRefresh.addActionListener(e -> ErrorHandler.run(this, () -> refreshTable()));
        btnAdd.addActionListener(e -> ErrorHandler.run(this, this::addEvent));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateEvent));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteEvent));
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
        for (DiscountEvent d : discountDAO.search(keyword)) {
            tableModel.addRow(new Object[]{d.getId(), d.getEventName(), d.getStartDate(), d.getEndDate(), d.getDiscountPercent() + "%",
                d.isActive() ? "Yes" : "No"});
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = discountDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());
        txtName.setText(selected.getEventName());
        txtStart.setText(selected.getStartDate().toString());
        txtEnd.setText(selected.getEndDate().toString());
        txtPercent.setText(selected.getDiscountPercent().toString());
        chkActive.setSelected(selected.isActive());
    }

    private void addEvent() {
        DiscountEvent d = readForm(new DiscountEvent());
        if (d == null) return;
        discountDAO.insert(d);
        JOptionPane.showMessageDialog(this, "Discount event added.");
        clearForm();
        refreshTable();
    }

    private void updateEvent() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a discount event first."); return; }
        DiscountEvent d = readForm(selected);
        if (d == null) return;
        discountDAO.update(d);
        JOptionPane.showMessageDialog(this, "Discount event updated.");
        clearForm();
        refreshTable();
    }

    private void deleteEvent() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete discount events")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a discount event first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\n" + selected.getEventName(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        discountDAO.delete(selected.getId());   // blocked by the foreign key if a reservation used it -> untick Active instead
        JOptionPane.showMessageDialog(this, "Discount event deleted.");
        clearForm();
        refreshTable();
    }

    private DiscountEvent readForm(DiscountEvent d) {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Event name is required.");
            return null;
        }
        if (name.length() > 50) {
            JOptionPane.showMessageDialog(this, "Event name is too long (50 characters max).");
            return null;
        }
        if (uniqueChecker.discountEventNameExists(name, d.getId())) {   // d.getId() is 0 for a new event
            JOptionPane.showMessageDialog(this, "A discount event with that name already exists.");
            return null;
        }
        LocalDate start = Validator.parseDate(txtStart.getText());
        LocalDate end = Validator.parseDate(txtEnd.getText());
        if (start == null || end == null) {
            JOptionPane.showMessageDialog(this, "Start and end dates must be valid dates in YYYY-MM-DD format.");
            return null;
        }
        if (end.isBefore(start)) {
            JOptionPane.showMessageDialog(this, "End date cannot be before the start date.");
            return null;
        }
        BigDecimal percent = Validator.parsePercent(txtPercent.getText());
        if (percent == null) {
            JOptionPane.showMessageDialog(this, "Discount percent must be a number greater than 0 and at most 100.");
            return null;
        }
        d.setEventName(name);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setDiscountPercent(percent);
        d.setActive(chkActive.isSelected());
        return d;
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtName.setText("");
        txtStart.setText("");
        txtEnd.setText("");
        txtPercent.setText("");
        chkActive.setSelected(true);
        table.clearSelection();
    }
}
