package ui;

import javax.swing.JOptionPane;
import java.awt.Component;

final class DeleteConfirmation {
    private DeleteConfirmation() { }

    static boolean confirm(Component parent, String recordDetails) {
        int first = JOptionPane.showConfirmDialog(parent,
            "Are you sure you want to delete this record?\n" + recordDetails,
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (first != JOptionPane.YES_OPTION) return false;

        int finalConfirmation = JOptionPane.showConfirmDialog(parent,
            "Are you really sure you want to delete this record?\nThis action cannot be undone.",
            "Final Delete Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return finalConfirmation == JOptionPane.YES_OPTION;
    }
}
