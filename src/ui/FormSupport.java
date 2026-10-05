package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Small helpers shared by every management form (ID box, double-click on a table row). */
public final class FormSupport {
    private FormSupport() { }

    /**
     * The read-only "ID" box shown at the top of every management form.
     * It is filled when a table row is selected and empty for a new record (the database assigns the ID).
     */
    public static JTextField idField() {
        JTextField f = new JTextField(6);
        f.setEditable(false);
        f.setFocusable(false);
        f.setBackground(new Color(230, 230, 230));
        f.setForeground(Color.BLACK);
        f.setHorizontalAlignment(SwingConstants.CENTER);
        f.setToolTipText("Assigned automatically by the database");
        return f;
    }

    /** Shows an id in the ID box, or clears it when id <= 0. */
    public static void showId(JTextField field, int id) {
        field.setText(id > 0 ? String.valueOf(id) : "");
    }

    /**
     * MouseListener: double-click a table row to run an action (for example "load this record into the form").
     * A single click still only selects the row.
     */
    public static void onDoubleClick(JTable table, Runnable action) {
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.rowAtPoint(e.getPoint()) >= 0) {
                    action.run();
                }
            }
        });
    }
}
