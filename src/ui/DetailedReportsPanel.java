package ui;

import dao.ReportQueryDAO;
import util.Validator;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Pick a report from the list, run it, see it in a JTable (with search). */
public class DetailedReportsPanel extends JPanel {
    private final ReportQueryDAO dao = new ReportQueryDAO();
    private final JComboBox<String> cboReport = new JComboBox<>(ReportQueryDAO.REPORT_NAMES);
    private final JTextField txtDate = new JTextField(LocalDate.now().toString(), 10);
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblSummary = new JLabel(" ");

    public DetailedReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnRun = new JButton("Generate Report");
        btnRun.addActionListener(e -> run());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Report:"));
        top.add(cboReport);
        top.add(new JLabel("Date (daily report, YYYY-MM-DD):"));
        top.add(txtDate);
        top.add(btnRun);

        add(top, BorderLayout.NORTH);
        add(SearchBar.wrap(table), BorderLayout.CENTER);
        add(lblSummary, BorderLayout.SOUTH);
    }

    private void run() {
        String name = (String) cboReport.getSelectedItem();
        LocalDate date = null;
        if ("Daily Transactions".equals(name)) {
            date = Validator.parseDate(txtDate.getText());
            if (date == null) {
                JOptionPane.showMessageDialog(this, "Enter a valid date, e.g. 2026-10-01.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        ReportQueryDAO.Result r = dao.run(name, date);
        model.setDataVector(r.rows.toArray(new Object[0][]), r.columns);
        lblSummary.setText(name + ": " + r.rows.size() + " record(s)");
        if (r.rows.isEmpty())
            JOptionPane.showMessageDialog(this, "No records found for this report.", "Report", JOptionPane.INFORMATION_MESSAGE);
    }
}
