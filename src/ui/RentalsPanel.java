package ui;

import util.Validator;

import util.ErrorHandler;
import util.AccessControl;

import dao.RentalDAO;
import util.UITheme;
import dao.DiscountEventDAO;
import dao.CustomerDAO;
import dao.VehicleDAO;
import dao.ReservationDAO;
import model.Reservation;
import model.Rental;
import model.DiscountEvent;
import model.Staff;
import model.Vehicle;
import model.Customer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

/** Admin/Employee screen: walk-in checkout, returns with billing, and receipts. */
public class RentalsPanel extends JPanel {

    private final RentalDAO rentalDAO = new RentalDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final DiscountEventDAO discountDAO = new DiscountEventDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Vehicle", "Customer", "Rented", "Due", "Rate/day", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = FormSupport.idField();   // read-only: id of the rental selected in the table

    /** One entry of the "approved reservation" box: shows a readable label, keeps the Reservation inside. */
    private static final class ReservationItem {
        final Reservation reservation;
        final String label;
        ReservationItem(Reservation reservation, String label) { this.reservation = reservation; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final String WALK_IN = "Walk-in (no reservation)";
    private final JComboBox<Object> cboReservation = new JComboBox<>();
    private boolean loadingCombos = false;   // true while the boxes are being refilled (ignore their events)

    private final JComboBox<Customer> cboCustomer = new JComboBox<>();
    private final JComboBox<Vehicle> cboVehicle = new JComboBox<>();
    private final JTextField txtDueDate = new JTextField(10);

    private Rental selected;

    public RentalsPanel(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);

        add(buildForm(), BorderLayout.NORTH);
        // MouseListener: double-click an ongoing rental to start "Return & Pay" for it
        FormSupport.onDoubleClick(table, () -> ErrorHandler.run(this, this::returnAndPay));
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selected = rowSelectedRental();
            FormSupport.showId(txtId, selected == null ? 0 : selected.getId());
            if (selected != null && "ongoing".equals(selected.getStatus())) txtDueDate.setText(selected.getDueDate().toString());
        });

        txtDueDate.setText(LocalDate.now().plusDays(1).toString());
        loadCombos();
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
        addField(form, gc, row++, "Selected Rental ID:", txtId);
        addField(form, gc, row++, "Approved reservation (optional):", cboReservation);
        cboReservation.addActionListener(e -> onReservationPicked());
        addField(form, gc, row++, "Customer:", cboCustomer);
        addField(form, gc, row++, "Vehicle (available only):", cboVehicle);
        addField(form, gc, row++, "Due Date (YYYY-MM-DD):", txtDueDate);

        JButton btnCheckOut = UITheme.primaryButton("Check Out (New Rental)");
        JButton btnReturn = UITheme.primaryButton("Return & Pay");
        JButton btnUpdate = UITheme.primaryButton("Update Due Date");
        JButton btnDelete = UITheme.primaryButton("Delete");
        JButton btnClear = UITheme.primaryButton("Clear");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnCheckOut.addActionListener(e -> ErrorHandler.run(this, this::checkOut));
        btnReturn.addActionListener(e -> ErrorHandler.run(this, this::returnAndPay));
        btnUpdate.addActionListener(e -> ErrorHandler.run(this, this::updateRental));
        btnDelete.addActionListener(e -> ErrorHandler.run(this, this::deleteRental));
        btnClear.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> { loadCombos(); refreshTable(); });

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnCheckOut);
        buttons.add(btnReturn);
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
        loadingCombos = true;
        try {
            cboCustomer.removeAllItems();
            Map<Integer, Customer> byId = new HashMap<>();
            for (Customer c : customerDAO.findAll()) {
                byId.put(c.getId(), c);
                if (c.isActive()) cboCustomer.addItem(c);
            }

            cboVehicle.removeAllItems();
            for (Vehicle v : vehicleDAO.findAll()) if ("available".equals(v.getStatus())) cboVehicle.addItem(v);

            cboReservation.removeAllItems();
            cboReservation.addItem(WALK_IN);
            for (Reservation r : reservationDAO.findApproved()) {
                Customer owner = byId.get(r.getCustomerId());
                String who = owner == null ? "customer #" + r.getCustomerId() : owner.getFirstName() + " " + owner.getLastName();
                cboReservation.addItem(new ReservationItem(r,
                    "#" + r.getId() + " - " + who + " - " + r.getVehicleName() + " (" + r.getStartDate() + " to " + r.getEndDate() + ")"));
            }
        } finally {
            loadingCombos = false;
        }
    }

    /** Picking an approved reservation fills in the customer, the vehicle and the due date for the staff member. */
    private void onReservationPicked() {
        if (loadingCombos) return;
        if (!(cboReservation.getSelectedItem() instanceof ReservationItem)) return;   // walk-in: leave the form alone
        Reservation res = ((ReservationItem) cboReservation.getSelectedItem()).reservation;

        for (int i = 0; i < cboCustomer.getItemCount(); i++) {
            if (cboCustomer.getItemAt(i).getId() == res.getCustomerId()) { cboCustomer.setSelectedIndex(i); break; }
        }
        boolean vehicleFound = false;
        for (int i = 0; i < cboVehicle.getItemCount(); i++) {
            if (cboVehicle.getItemAt(i).getId() == res.getVehicleId()) { cboVehicle.setSelectedIndex(i); vehicleFound = true; break; }
        }
        txtDueDate.setText(res.getEndDate().toString());
        if (!vehicleFound) {
            JOptionPane.showMessageDialog(this,
                "The reserved vehicle (" + res.getVehicleName() + ") is not available right now.\n"
              + "Wait until it is returned or set it back to \"available\" in the Vehicles screen.",
                "Vehicle Not Available", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Rental r : rentalDAO.search(keyword)) {
            tableModel.addRow(new Object[]{
                r.getId(), r.getVehicleName(), r.getCustomerName(), r.getRentOutDate().toLocalDate(), r.getDueDate(),
                UITheme.peso(r.getDailyRateSnapshot()), r.getStatus()
            });
        }
    }

    private Rental rowSelectedRental() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        return rentalDAO.findById(id);
    }

    private void checkOut() {
        Customer cust = (Customer) cboCustomer.getSelectedItem();
        Vehicle veh = (Vehicle) cboVehicle.getSelectedItem();
        if (cust == null || veh == null) {
            JOptionPane.showMessageDialog(this, "Select a customer and an available vehicle.");
            return;
        }
        LocalDate due = Validator.parseDate(txtDueDate.getText());
        if (due == null) {
            JOptionPane.showMessageDialog(this, "Due date must be a valid date in YYYY-MM-DD format.");
            return;
        }
        if (due.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Due date cannot be in the past.");
            return;
        }

        Reservation fromReservation = (cboReservation.getSelectedItem() instanceof ReservationItem)
            ? ((ReservationItem) cboReservation.getSelectedItem()).reservation : null;
        if (fromReservation != null
                && (fromReservation.getCustomerId() != cust.getId() || fromReservation.getVehicleId() != veh.getId())) {
            JOptionPane.showMessageDialog(this,
                "The customer and vehicle must be the ones on the approved reservation.\n"
              + "Choose \"" + WALK_IN + "\" if this is a different rental.");
            return;
        }

        // A reservation keeps the discount that was promised when it was made; a walk-in gets today's active event.
        DiscountEvent active = null;
        if (fromReservation != null && fromReservation.getDiscountId() != null) {
            active = discountDAO.findById(fromReservation.getDiscountId());
        } else if (fromReservation == null) {
            active = discountDAO.findActiveForDate(LocalDate.now());
        }

        Rental r = new Rental();
        if (fromReservation != null) r.setReservationId(fromReservation.getId());
        r.setCustomerId(cust.getId());
        r.setVehicleId(veh.getId());
        r.setStaffId(loggedInStaff.getId());
        r.setDueDate(due);
        r.setDailyRateSnapshot(veh.getDailyRate());
        r.setDiscountPercentSnapshot(active != null ? active.getDiscountPercent() : BigDecimal.ZERO);
        r.setStatus("ongoing");

        rentalDAO.checkOut(r);
        JOptionPane.showMessageDialog(this, "Vehicle checked out." +
            (active != null ? " Holiday discount applied: " + active.getEventName() + " (" + active.getDiscountPercent() + "%)" : ""));
        loadCombos();
        refreshTable();
    }

    private void returnAndPay() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an ongoing rental from the table first."); return; }
        if (!"ongoing".equals(selected.getStatus())) { JOptionPane.showMessageDialog(this, "This rental is already closed."); return; }

        JTextField txtNotes = new JTextField();
        JTextField txtDamage = new JTextField("0");
        // Payment method: JRadioButtons in one ButtonGroup (only one can be chosen)
        String[] methods = {"cash", "card", "gcash", "bank_transfer"};
        ButtonGroup methodGroup = new ButtonGroup();
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JRadioButton[] methodButtons = new JRadioButton[methods.length];
        for (int i = 0; i < methods.length; i++) {
            methodButtons[i] = new JRadioButton(methods[i], i == 0);   // "cash" is the default
            methodGroup.add(methodButtons[i]);
            methodPanel.add(methodButtons[i]);
        }
        JPanel dialogPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        dialogPanel.add(new JLabel("Condition notes:"));
        dialogPanel.add(txtNotes);
        dialogPanel.add(new JLabel("Damage fee (0 if none):"));
        dialogPanel.add(txtDamage);
        dialogPanel.add(new JLabel("Payment method:"));
        dialogPanel.add(methodPanel);

        int confirm = JOptionPane.showConfirmDialog(this, dialogPanel, "Return Vehicle #" + selected.getId(),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        BigDecimal damageFee = Validator.parseNonNegative(txtDamage.getText());
        if (damageFee == null) {
            JOptionPane.showMessageDialog(this, "Damage fee must be a number (0 or more).");
            return;
        }

        String receipt = rentalDAO.returnAndPay(selected, LocalDate.now(), txtNotes.getText().trim(),
            damageFee, loggedInStaff.getId(), selectedMethod(methodButtons));

        JTextArea area = new JTextArea(receipt, 16, 40);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Receipt", JOptionPane.PLAIN_MESSAGE);

        selected = null;
        loadCombos();
        refreshTable();
    }

    /** UPDATE: change the due date of the selected ongoing rental. */
    private void updateRental() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a rental from the table first."); return; }
        if (!"ongoing".equals(selected.getStatus())) {
            JOptionPane.showMessageDialog(this, "Only ongoing rentals can be changed. Closed rentals are kept as billing history.");
            return;
        }
        LocalDate due = Validator.parseDate(txtDueDate.getText());
        if (due == null) {
            JOptionPane.showMessageDialog(this, "Due date must be a valid date in YYYY-MM-DD format.");
            return;
        }
        if (due.isBefore(selected.getRentOutDate().toLocalDate())) {
            JOptionPane.showMessageDialog(this, "The due date cannot be before the day the vehicle was rented out.");
            return;
        }
        rentalDAO.updateDueDate(selected.getId(), due);
        JOptionPane.showMessageDialog(this, "Rental updated.");
        clearForm();
        loadCombos();
        refreshTable();
    }

    /** DELETE (admin only, with confirmation): removes a rental that was started by mistake. */
    private void deleteRental() {
        if (!AccessControl.requireAdmin(this, loggedInStaff, "delete rentals")) return;
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select a rental from the table first."); return; }
        if (!"ongoing".equals(selected.getStatus())) {
            JOptionPane.showMessageDialog(this, "Only ongoing rentals can be deleted. Closed rentals are kept as billing history.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?\nRental #" + selected.getId() + " - " + selected.getVehicleName()
              + " - " + selected.getCustomerName() + "\nThe vehicle will be set back to available.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        rentalDAO.deleteOngoing(selected.getId());
        JOptionPane.showMessageDialog(this, "Rental deleted.");
        clearForm();
        loadCombos();
        refreshTable();
    }

    /** Clear button: resets the form and the table selection. */
    private void clearForm() {
        selected = null;
        txtId.setText("");
        txtDueDate.setText(LocalDate.now().plusDays(1).toString());
        table.clearSelection();
        if (cboReservation.getItemCount() > 0) cboReservation.setSelectedIndex(0);
        if (cboCustomer.getItemCount() > 0) cboCustomer.setSelectedIndex(0);
        if (cboVehicle.getItemCount() > 0) cboVehicle.setSelectedIndex(0);
    }

    /** Text of the radio button that is currently chosen. */
    private static String selectedMethod(JRadioButton[] buttons) {
        for (JRadioButton b : buttons) if (b.isSelected()) return b.getText();
        return "cash";
    }
}
