import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Reports screen. Click "Generate Chart" to draw income-by-category. MP5 date-range box is below it. */
public class ReportsPanel extends JPanel {

    private final ReportDAO reportDAO = new ReportDAO();
    private final BarChartPanel chart = new BarChartPanel();

    private final JTextField txtFrom = new JTextField("2026-01-01", 10);
    private final JTextField txtTo   = new JTextField("2026-12-31", 10);
    private final JLabel lblTotal = new JLabel("Total Income: —");

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnGenerate = new JButton("Generate Chart (Income by Category)");
        btnGenerate.addActionListener(e -> generateChart());

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
        btnCompute.addActionListener(e -> computeIncome());
        dateRangePanel.add(btnCompute);
        dateRangePanel.add(lblTotal);
        add(dateRangePanel, BorderLayout.SOUTH);
    }

    private void generateChart() {
        chart.setData(reportDAO.incomeByCategory(), "Income by Vehicle Category");
    }

    private void computeIncome() {
        try {
            LocalDate from = LocalDate.parse(txtFrom.getText().trim());
            LocalDate to = LocalDate.parse(txtTo.getText().trim());
            BigDecimal total = reportDAO.totalIncomeBetween(from, to);
            lblTotal.setText("Total Income: \u20B1" + total);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter valid dates, e.g. 2026-01-01");
        }
    }
}
