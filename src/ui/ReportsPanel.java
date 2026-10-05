package ui;

import dao.ReportDAO;
import util.ErrorHandler;
import util.Validator;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Reports screen. Click "Generate Chart" to draw income-by-category. MP5 date-range box is below it. */
public class ReportsPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();
    private final BarChartPanel chart = new BarChartPanel();

    // default range = the current year, so the screen is still correct next year
    private final JTextField txtFrom = new JTextField(LocalDate.now().withDayOfYear(1).toString(), 10);
    private final JTextField txtTo   = new JTextField(LocalDate.now().withMonth(12).withDayOfMonth(31).toString(), 10);
    private final JLabel lblTotal = new JLabel("Total Income: —");

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnGenerate = new JButton("Generate Chart (Income by Category)");
        btnGenerate.addActionListener(e -> ErrorHandler.run(this, this::generateChart));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(btnGenerate);
        add(top, BorderLayout.NORTH);

        chart.setPreferredSize(new Dimension(600, 350));
        add(chart, BorderLayout.CENTER);

        JPanel dateRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateRangePanel.add(new JLabel("From (YYYY-MM-DD):"));
        dateRangePanel.add(txtFrom);
        dateRangePanel.add(new JLabel("To:"));
        dateRangePanel.add(txtTo);
        JButton btnCompute = new JButton("Compute Income");
        btnCompute.addActionListener(e -> ErrorHandler.run(this, this::computeIncome));
        dateRangePanel.add(btnCompute);
        dateRangePanel.add(lblTotal);
        add(dateRangePanel, BorderLayout.SOUTH);
    }

    private void generateChart() {
        chart.setData(reportDAO.incomeByCategory(), "Income by Vehicle Category");
    }

    private void computeIncome() {
        LocalDate from = Validator.parseDate(txtFrom.getText());
        LocalDate to = Validator.parseDate(txtTo.getText());
        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "Enter valid dates in YYYY-MM-DD format, e.g. " + LocalDate.now().withDayOfYear(1));
            return;
        }
        if (from.isAfter(to)) {
            JOptionPane.showMessageDialog(this, "The 'From' date cannot be after the 'To' date.");
            return;
        }
        BigDecimal total = reportDAO.totalIncomeBetween(from, to);
        lblTotal.setText("Total Income: \u20B1" + total);
    }
}
