package ui;

import util.ErrorHandler;

import dao.ReservationDAO;
import model.Reservation;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Staff-side screen: review reservations customers submitted through the portal. */
public class PendingOrdersPanel extends JPanel {

    private final ReservationDAO reservationDAO = new ReservationDAO();

    private final DefaultTableModel model =
        new DefaultTableModel(new Object[]{"ID", "Vehicle", "From", "To", "Discount", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(model);
    private final JTextField txtId = FormSupport.idField();   // read-only: id of the selected reservation

    public PendingOrdersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(SearchBar.wrap(table, this::runSearch), BorderLayout.CENTER);

        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JButton btnRefresh = new JButton("Refresh");
        btnApprove.addActionListener(e -> ErrorHandler.run(this, () -> updateStatus("approved")));
        btnReject.addActionListener(e -> ErrorHandler.run(this, () -> updateStatus("rejected")));
        btnRefresh.addActionListener(e -> refresh());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            FormSupport.showId(txtId, row < 0 ? 0 : (int) model.getValueAt(table.convertRowIndexToModel(row), 0));
        });
        // MouseListener: double-click a row to see the request's full details
        FormSupport.onDoubleClick(table, this::showDetails);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("ID:"));
        top.add(txtId);
        top.add(btnApprove);
        top.add(btnReject);
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);

        refresh();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { refresh(); }
        });
    }

    /** Keyword currently typed in the search box ("" = show all). */
    private String keyword = "";

    private void runSearch(String text) {
        keyword = text;
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for (Reservation r : reservationDAO.searchPending(keyword)) {
            model.addRow(new Object[]{
                r.getId(), r.getVehicleName(), r.getStartDate(), r.getEndDate(),
                r.getDiscountEventName() != null ? r.getDiscountEventName() : "—", r.getStatus()
            });
        }
    }

    private void updateStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation first."); return; }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        Reservation r = reservationDAO.findById(id);
        if (r == null) { JOptionPane.showMessageDialog(this, "That reservation no longer exists."); refresh(); return; }
        if ("approved".equals(newStatus) && reservationDAO.vehicleBookedBetween(r.getVehicleId(), r.getStartDate(), r.getEndDate(), r.getId())) {
            JOptionPane.showMessageDialog(this,
                "This vehicle is already booked (approved) for overlapping dates.\nReject this request or pick other dates.",
                "Vehicle Already Booked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        r.setStatus(newStatus);
        reservationDAO.update(r);
        JOptionPane.showMessageDialog(this, "Reservation #" + id + " marked " + newStatus + ".");
        refresh();
    }

    private void showDetails() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        Reservation r = reservationDAO.findById(id);
        if (r == null) { JOptionPane.showMessageDialog(this, "That reservation no longer exists."); refresh(); return; }
        JOptionPane.showMessageDialog(this,
            "Reservation #" + r.getId() + "\nVehicle: " + r.getVehicleName() +
            "\nFrom: " + r.getStartDate() + "   To: " + r.getEndDate() +
            "\nDiscount: " + (r.getDiscountEventName() != null ? r.getDiscountEventName() : "none") +
            "\nStatus: " + r.getStatus(), "Reservation Details", JOptionPane.INFORMATION_MESSAGE);
    }
}
