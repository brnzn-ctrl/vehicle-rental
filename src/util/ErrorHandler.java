package util;

import javax.swing.*;
import java.awt.Component;
import java.sql.SQLException;

/** Turns SQL/unexpected exceptions into friendly dialogs, and catches anything nobody else caught. */
public final class ErrorHandler {
    private ErrorHandler() { }

    public static String friendlyMessage(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            if (x instanceof SQLException) {
                String state = ((SQLException) x).getSQLState();
                if (state == null) state = "";
                if (state.equals("23505")) return "Duplicate record: that value already exists.";
                if (state.equals("23503")) return "This record is linked to other records (invalid reference or still in use), so the action was blocked.";
                if (state.equals("23513")) return "A value is not allowed by the database rules (check status/role fields).";
                if (state.equals("22001")) return "One of the values is too long for its field.";
                if (state.equals("23502")) return "A required value is missing.";
                if (state.startsWith("VR")) return x.getMessage();   // our own business-rule messages
                if (state.startsWith("08") || state.equals("XJ040") || state.equals("XSDB6") || state.equals("XJ015"))
                    return "Cannot connect to the database. Make sure no other copy of the program is running and derby.jar is on the classpath.";
            }
        }
        return "Unexpected error: " + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
    }

    public static void show(Component parent, Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(parent, friendlyMessage(t), "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Runs a button action; any runtime/SQL failure becomes a friendly dialog instead of a silent crash. */
    public static void run(Component parent, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            show(parent, ex);
        }
    }

    /** Call once at startup: any exception that escapes a button handler gets a dialog instead of silence. */
    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((th, ex) -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> show(null, ex));
        });
    }
}
