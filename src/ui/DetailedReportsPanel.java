package ui;

import dao.ReportQueryDAO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Pick a report from the list, run it, see it in a JTable (with search). */
public class DetailedReportsPanel extends JPanel {
    private final ReportQueryDAO dao = new ReportQueryDAO();
    private final JComboBox<String> cboReport = new JComboBox<>(ReportQueryDAO.REPORT_NAMES);
    private final DatePickerField txtDate = new DatePickerField(LocalDate.now());
    private final JLabel lblDate = new JLabel("Date:");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblSummary = new JLabel(" ");
    private boolean generated;

    public DetailedReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnRun = new JButton("Generate Report");
        btnRun.addActionListener(e -> run());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Report:"));
        top.add(cboReport);
        top.add(lblDate);
        top.add(txtDate);
        top.add(btnRun);

        add(top, BorderLayout.NORTH);
        add(SearchBar.wrap(table), BorderLayout.CENTER);
        add(lblSummary, BorderLayout.SOUTH);
        cboReport.addActionListener(e -> updateDateVisibility());
        updateDateVisibility();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                if (generated) run(false);
            }
        });
    }

    private void run() {
        run(true);
    }

    private void run(boolean notifyIfEmpty) {
        String name = (String) cboReport.getSelectedItem();
        LocalDate date = null;
        if ("Daily Transactions".equals(name)) {
            date = txtDate.getDate();
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Choose a report date.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        ReportQueryDAO.Result r = dao.run(name, date);
        model.setDataVector(r.rows.toArray(new Object[0][]), r.columns);
        lblSummary.setText(name + ": " + r.rows.size() + " record(s)");
        generated = true;
        if (notifyIfEmpty && r.rows.isEmpty())
            JOptionPane.showMessageDialog(this, "No records found for this report.", "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateDateVisibility() {
        boolean dailyReport = "Daily Transactions".equals(cboReport.getSelectedItem());
        lblDate.setVisible(dailyReport);
        txtDate.setVisible(dailyReport);
        revalidate();
        repaint();
    }
}
