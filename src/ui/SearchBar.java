package ui;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Search box for any JTable. Usage (replaces add(new JScrollPane(table), ...)):

 */
public final class SearchBar {
    private SearchBar() { }

    /** Search box that runs a SQL LIKE query in the database through onSearch(keyword). */
    public static JPanel wrap(JTable table, Consumer<String> onSearch) {
        table.setAutoCreateRowSorter(true);          // click a column heading to sort

        JTextField txtSearch = new JTextField(22);
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");
        JLabel lblCount = new JLabel(" ");

        Runnable apply = () -> {
            String text = txtSearch.getText().trim();
            onSearch.accept(text);                    // DAO runs: SELECT ... WHERE ... LIKE ? ORDER BY ...
            lblCount.setText(text.isEmpty() ? " " : table.getRowCount() + " record(s) found");
        };
        btnSearch.addActionListener(e -> util.ErrorHandler.run(table, apply));
        txtSearch.addActionListener(e -> util.ErrorHandler.run(table, apply));
        btnReset.addActionListener(e -> { txtSearch.setText(""); util.ErrorHandler.run(table, apply); });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Search:"));
        bar.add(txtSearch);
        bar.add(btnSearch);
        bar.add(btnReset);
        bar.add(lblCount);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /** Local filter over the rows already in the table (no database call). */
    public static JPanel wrap(JTable table) {
        TableModel model = table.getModel();
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JComboBox<String> cboColumn = new JComboBox<>();
        Runnable fillColumns = () -> {
            cboColumn.removeAllItems();
            cboColumn.addItem("All columns");
            for (int i = 0; i < model.getColumnCount(); i++) cboColumn.addItem(model.getColumnName(i));
        };
        fillColumns.run();
        // report tables change their columns at run time -> keep the chooser in sync
        model.addTableModelListener(e -> {
            if (e.getFirstRow() == javax.swing.event.TableModelEvent.HEADER_ROW) fillColumns.run();
        });
        JTextField txtSearch = new JTextField(18);
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");
        JLabel lblCount = new JLabel(" ");

        Runnable apply = () -> {
            String text = txtSearch.getText().trim();
            if (text.isEmpty()) { sorter.setRowFilter(null); lblCount.setText(" "); return; }
            String regex = "(?i)" + Pattern.quote(text);
            int col = cboColumn.getSelectedIndex() - 1;
            sorter.setRowFilter(col < 0 ? RowFilter.regexFilter(regex) : RowFilter.regexFilter(regex, col));
            lblCount.setText(table.getRowCount() + " record(s) found");
        };
        btnSearch.addActionListener(e -> apply.run());
        txtSearch.addActionListener(e -> apply.run());
        btnReset.addActionListener(e -> { txtSearch.setText(""); cboColumn.setSelectedIndex(0); apply.run(); });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Search:"));
        bar.add(txtSearch);
        bar.add(new JLabel("in"));
        bar.add(cboColumn);
        bar.add(btnSearch);
        bar.add(btnReset);
        bar.add(lblCount);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
