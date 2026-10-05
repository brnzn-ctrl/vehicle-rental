package ui;

import dao.CustomerDAO;
import dao.DiscountEventDAO;
import dao.ReservationDAO;
import dao.VehicleDAO;
import model.Customer;
import model.DiscountEvent;
import model.Reservation;
import model.Staff;
import model.Vehicle;
import util.AccessControl;
import util.ErrorHandler;
import util.UITheme;
import util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin/Employee screen: full CRUD for reservations (phone-in / walk-in bookings), search, clear.
 * The customer-portal "Pending Customer Orders" screen still handles approve/reject of online requests.
 */
public class ReservationsPanel extends JPanel {

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final DiscountEventDAO discountDAO = new DiscountEventDAO();
    private final Staff loggedInStaff;

    private static final String[] STATUSES = {"pending", "approved", "rejected", "cancelled", "converted"};

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Customer", "Vehicle", "Start", "End", "Status", "Discount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();

    /** Combo entry for the optional discount event. */
    private static final class DiscountItem {
        final Integer id; final String label;
        DiscountItem(Integer id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    private final JComboBox<Customer> cboCustomer = new JComboBox<>();
    private final JComboBox<Vehicle> cboVehicle = new JComboBox<>();
    private final JTextField txtStart = new JTextField(10);
    private final JTextField txtEnd = new JTextField(10);
    private final JComboBox<DiscountItem> cboDiscount = new JComboBox<>();
    private final JComboBox<String> cboStatus = new JComboBox<>(STATUSES);

    private final Map<Integer, String> customerNames = new HashMap<>();
    private Reservation selected;
    private String keyword = "";

    public ReservationsPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        loadCombos();
        clearForm();
        refreshTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { loadCombos(); refreshTable(); }
        });
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Reservation ID:", txtId);
        addField(form, gc, row++, "Customer:", cboCustomer);
        addField(form, gc, row++, "Vehicle:", cboVehicle);
        addField(form, gc, row++, "Start Date (YYYY-MM-DD):", txtStart);
        addField(form, gc, row++, "End Date (YYYY-MM-DD):", txtEnd);
        addField(form, gc, row++, "Discount event:", cboDiscount);
        addField(form, gc, row++, "Status:", cboStatus);

        JButton btnSave = UITheme.primaryButton("Save (Add)");
        JButton btnUpdate = UITheme.primaryButton("Update");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnSave.addActionListener(e -> ErrorHandler.run(this, this::addReservation));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateReservation));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteReservation));
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> { loadCombos(); refreshTable(); });

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

    private void loadCombos() {
        Object keepCustomer = cboCustomer.getSelectedItem();
        customerNames.clear();
        cboCustomer.removeAllItems();
        for (Customer c : customerDAO.findAll()) {
            customerNames.put(c.getId(), c.getFirstName() + " " + c.getLastName());
            if (c.isActive()) cboCustomer.addItem(c);
        }
        if (keepCustomer != null) cboCustomer.setSelectedItem(keepCustomer);

        cboVehicle.removeAllItems();
        for (Vehicle v : vehicleDAO.findAll()) {
            if (!"inactive".equals(v.getStatus()) && !"maintenance".equals(v.getStatus())) cboVehicle.addItem(v);
        }

        cboDiscount.removeAllItems();
        cboDiscount.addItem(new DiscountItem(null, "None"));
        for (DiscountEvent d : discountDAO.findAll()) {
            cboDiscount.addItem(new DiscountItem(d.getId(), d.getEventName() + " (" + d.getDiscountPercent() + "%)"));
        }
    }

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Reservation r : reservationDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                r.getId(), customerNames.getOrDefault(r.getCustomerId(), "#" + r.getCustomerId()),
                r.getVehicleName(), r.getStartDate(), r.getEndDate(), r.getStatus(),
                r.getDiscountEventName() == null ? "-" : r.getDiscountEventName()
            });
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        selected = reservationDAO.findById(id);
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "That record no longer exists. The list will be refreshed.");
            refreshTable();
            return;
        }
        FormSupport.showId(txtId, selected.getId());
        selectOrAddCustomer(selected.getCustomerId());
        selectOrAddVehicle(selected.getVehicleId());
        txtStart.setText(selected.getStartDate().toString());
        txtEnd.setText(selected.getEndDate().toString());
        cboStatus.setSelectedItem(selected.getStatus());
        cboDiscount.setSelectedIndex(0);
        for (int i = 0; i < cboDiscount.getItemCount(); i++) {
            Integer did = cboDiscount.getItemAt(i).id;
            if (did != null && did.equals(selected.getDiscountId())) { cboDiscount.setSelectedIndex(i); break; }
        }
    }

    /** The dropdown only lists active customers; an old reservation may belong to an inactive one, so add it back. */
    private void selectOrAddCustomer(int customerId) {
        for (int i = 0; i < cboCustomer.getItemCount(); i++)
            if (cboCustomer.getItemAt(i).getId() == customerId) { cboCustomer.setSelectedIndex(i); return; }
        Customer c = customerDAO.findById(customerId);
        if (c != null) { cboCustomer.addItem(c); cboCustomer.setSelectedItem(c); }
    }

    /** Same for vehicles that are inactive / in maintenance: never leave a different vehicle selected by accident. */
    private void selectOrAddVehicle(int vehicleId) {
        for (int i = 0; i < cboVehicle.getItemCount(); i++)
            if (cboVehicle.getItemAt(i).getId() == vehicleId) { cboVehicle.setSelectedIndex(i); return; }
        Vehicle v = vehicleDAO.findById(vehicleId);
        if (v != null) { cboVehicle.addItem(v); cboVehicle.setSelectedItem(v); }
    }

    /** Validates the form. Returns false (after showing a message) if anything is wrong. */
    private boolean validateForm(boolean adding) {
        if (cboCustomer.getSelectedItem() == null || cboVehicle.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Select a customer and a vehicle.");
            return false;
        }
        LocalDate start = Validator.parseDate(txtStart.getText());
        LocalDate end = Validator.parseDate(txtEnd.getText());
        if (start == null || end == null) {
            JOptionPane.showMessageDialog(this, "Start and end dates must be valid dates in YYYY-MM-DD format.");
            return false;
        }
        if (end.isBefore(start)) {
            JOptionPane.showMessageDialog(this, "The end date cannot be before the start date.");
            return false;
        }
        if (adding && start.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "The start date cannot be in the past.");
            return false;
        }
        if ("converted".equals(cboStatus.getSelectedItem()) && (adding || selected == null || !"converted".equals(selected.getStatus()))) {
            JOptionPane.showMessageDialog(this, "\"converted\" is set automatically when a rental is created from the reservation.");
            return false;
        }
        return true;
    }

    private void fill(Reservation r) {
        Customer cust = (Customer) cboCustomer.getSelectedItem();
        Vehicle veh = (Vehicle) cboVehicle.getSelectedItem();
        r.setCustomerId(cust.getId());
        r.setVehicleId(veh.getId());
        r.setStartDate(Validator.parseDate(txtStart.getText()));
        r.setEndDate(Validator.parseDate(txtEnd.getText()));
        r.setStatus((String) cboStatus.getSelectedItem());
        r.setDiscountId(((DiscountItem) cboDiscount.getSelectedItem()).id);
    }

    private void addReservation() {
        if (!validateForm(true)) return;
        Reservation r = new Reservation();
        fill(r);
        if (reservationDAO.customerHasOverlap(r.getCustomerId(), r.getVehicleId(), r.getStartDate(), r.getEndDate())) {
            JOptionPane.showMessageDialog(this, "Duplicate: this customer already has a pending/approved booking of this vehicle in those dates.");
            return;
        }
        if ("approved".equals(r.getStatus()) && reservationDAO.vehicleBookedBetween(r.getVehicleId(), r.getStartDate(), r.getEndDate(), 0)) {
            JOptionPane.showMessageDialog(this, "That vehicle is already promised to another customer in those dates.");
            return;
        }
        reservationDAO.insert(r);
        JOptionPane.showMessageDialog(this, "Reservation saved.");
        clearForm();
        refreshTable();
    }

    private void updateReservation() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a reservation first."); return; }
        if ("converted".equals(selected.getStatus())) {
            JOptionPane.showMessageDialog(this, "This reservation was already turned into a rental and can no longer be edited.");
            return;
        }
        if (!validateForm(false)) return;
        fill(selected);
        if ("approved".equals(selected.getStatus())
                && reservationDAO.vehicleBookedBetween(selected.getVehicleId(), selected.getStartDate(), selected.getEndDate(), selected.getId())) {
            JOptionPane.showMessageDialog(this, "That vehicle is already promised to another customer in those dates.");
            return;
        }
        reservationDAO.update(selected);
        JOptionPane.showMessageDialog(this, "Reservation updated.");
        clearForm();
        refreshTable();
    }

    private void deleteReservation() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete reservations")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a reservation first."); return; }
        if ("converted".equals(selected.getStatus())) {
            JOptionPane.showMessageDialog(this, "This reservation is linked to a rental, so it is kept for the rental history.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\nReservation #" + selected.getId(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        reservationDAO.delete(selected.getId());
        JOptionPane.showMessageDialog(this, "Reservation deleted.");
        clearForm();
        refreshTable();
    }

    private void clearForm() {
        selected = null;
        txtId.setText("");
        if (cboCustomer.getItemCount() > 0) cboCustomer.setSelectedIndex(0);
        if (cboVehicle.getItemCount() > 0) cboVehicle.setSelectedIndex(0);
        txtStart.setText(LocalDate.now().toString());
        txtEnd.setText(LocalDate.now().plusDays(1).toString());
        if (cboDiscount.getItemCount() > 0) cboDiscount.setSelectedIndex(0);
        cboStatus.setSelectedItem("pending");
        table.clearSelection();
    }
}
