package ui;

import dao.ReturnDAO;
import model.ReturnRecord;
import model.Staff;
import util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Read-only return history; new returns are recorded from Rentals with Return & Pay. */
public class ReturnsPanel extends JPanel {
    private final ReturnDAO returnDAO = new ReturnDAO();
    private final DefaultTableModel tableModel =
        new DefaultTableModel(new Object[]{"Return ID", "Rental ID", "Customer", "Vehicle", "Returned", "Late days",
                                           "Late fee", "Damage fee", "Condition notes", "Received by"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    private final JTable table = new JTable(tableModel);
    private String keyword = "";

    public ReturnsPanel(Staff loggedInStaff) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);
        add(SearchBar.wrap(table, this::runSearch, this::refreshTable), BorderLayout.CENTER);
        refreshTable();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { refreshTable(); }
        });
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
}
