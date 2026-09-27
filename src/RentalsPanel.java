import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin/Employee screen: walk-in checkout, returns with billing, and receipts. */
public class RentalsPanel extends JPanel {

    private final RentalDAO rentalDAO = new RentalDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final DiscountEventDAO discountDAO = new DiscountEventDAO();
    private final Staff loggedInStaff;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"ID", "Vehicle", "Customer", "Rented", "Due", "Rate/day", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);

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
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selected = rowSelectedRental();
        });

        txtDueDate.setText(LocalDate.now().plusDays(1).toString());
        loadCombos();
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_DARK);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gc, row++, "Customer:", cboCustomer);
        addField(form, gc, row++, "Vehicle (available only):", cboVehicle);
        addField(form, gc, row++, "Due Date (YYYY-MM-DD):", txtDueDate);

        JButton btnCheckOut = UITheme.primaryButton("Check Out (New Rental)");
        JButton btnReturn = UITheme.primaryButton("Return & Pay");
        JButton btnRefresh = UITheme.primaryButton("Refresh");
        btnCheckOut.addActionListener(e -> checkOut());
        btnReturn.addActionListener(e -> returnAndPay());
        btnRefresh.addActionListener(e -> { loadCombos(); refreshTable(); });

        JPanel buttons = new JPanel();
        buttons.setBackground(UITheme.BG_DARK);
        buttons.add(btnCheckOut);
        buttons.add(btnReturn);
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
        cboCustomer.removeAllItems();
        for (Customer c : customerDAO.findAll()) if (c.active) cboCustomer.addItem(c);

        cboVehicle.removeAllItems();
        for (Vehicle v : vehicleDAO.findAll()) if ("available".equals(v.status)) cboVehicle.addItem(v);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Rental r : rentalDAO.findAll()) {
            tableModel.addRow(new Object[]{
                r.id, r.vehicleName, r.customerName, r.rentOutDate.toLocalDate(), r.dueDate,
                UITheme.peso(r.dailyRateSnapshot), r.status
            });
        }
    }

    private Rental rowSelectedRental() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return rentalDAO.findById(id);
    }

    private void checkOut() {
        Customer cust = (Customer) cboCustomer.getSelectedItem();
        Vehicle veh = (Vehicle) cboVehicle.getSelectedItem();
        if (cust == null || veh == null) {
            JOptionPane.showMessageDialog(this, "Select a customer and an available vehicle.");
            return;
        }
        LocalDate due;
        try {
            due = LocalDate.parse(txtDueDate.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Due date must be in YYYY-MM-DD format.");
            return;
        }

        DiscountEvent active = discountDAO.findActiveForDate(LocalDate.now());

        Rental r = new Rental();
        r.customerId = cust.id;
        r.vehicleId = veh.id;
        r.staffId = loggedInStaff.id;
        r.dueDate = due;
        r.dailyRateSnapshot = veh.dailyRate;
        r.discountPercentSnapshot = active != null ? active.discountPercent : BigDecimal.ZERO;
        r.status = "ongoing";

        rentalDAO.checkOut(r);
        JOptionPane.showMessageDialog(this, "Vehicle checked out." +
            (active != null ? " Holiday discount applied: " + active.eventName + " (" + active.discountPercent + "%)" : ""));
        loadCombos();
        refreshTable();
    }

    private void returnAndPay() {
        if (selected == null) { JOptionPane.showMessageDialog(this, "Select an ongoing rental from the table first."); return; }
        if (!"ongoing".equals(selected.status)) { JOptionPane.showMessageDialog(this, "This rental is already closed."); return; }

        JTextField txtNotes = new JTextField();
        JTextField txtDamage = new JTextField("0");
        JComboBox<String> cboMethod = new JComboBox<>(new String[]{"cash", "card", "gcash", "bank_transfer"});
        JPanel dialogPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        dialogPanel.add(new JLabel("Condition notes:"));
        dialogPanel.add(txtNotes);
        dialogPanel.add(new JLabel("Damage fee (0 if none):"));
        dialogPanel.add(txtDamage);
        dialogPanel.add(new JLabel("Payment method:"));
        dialogPanel.add(cboMethod);

        int confirm = JOptionPane.showConfirmDialog(this, dialogPanel, "Return Vehicle #" + selected.id,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) return;

        BigDecimal damageFee;
        try {
            damageFee = new BigDecimal(txtDamage.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Damage fee must be a number.");
            return;
        }

        String receipt = rentalDAO.returnAndPay(selected, LocalDate.now(), txtNotes.getText().trim(),
            damageFee, loggedInStaff.id, (String) cboMethod.getSelectedItem());

        JTextArea area = new JTextArea(receipt, 16, 40);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Receipt", JOptionPane.PLAIN_MESSAGE);

        selected = null;
        loadCombos();
        refreshTable();
    }
}
