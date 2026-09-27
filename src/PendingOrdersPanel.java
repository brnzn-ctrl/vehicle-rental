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

    public PendingOrdersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JButton btnRefresh = new JButton("Refresh");
        btnApprove.addActionListener(e -> updateStatus("approved"));
        btnReject.addActionListener(e -> updateStatus("rejected"));
        btnRefresh.addActionListener(e -> refresh());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnApprove);
        top.add(btnReject);
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);

        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for (Reservation r : reservationDAO.findPending()) {
            model.addRow(new Object[]{
                r.id, r.vehicleName, r.startDate, r.endDate,
                r.discountEventName != null ? r.discountEventName : "—", r.status
            });
        }
    }

    private void updateStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a reservation first."); return; }
        int id = (int) model.getValueAt(row, 0);
        Reservation r = reservationDAO.findById(id);
        r.status = newStatus;
        reservationDAO.update(r);
        JOptionPane.showMessageDialog(this, "Reservation #" + id + " marked " + newStatus + ".");
        refresh();
    }
}
