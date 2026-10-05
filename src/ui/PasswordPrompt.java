package ui;

import javax.swing.*;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;

/** Password entry dialog that uses JPasswordField (the text is never shown on screen). */
public final class PasswordPrompt {

    private PasswordPrompt() { }

    /** Returns the new password, or null if the user cancelled / the two boxes did not match. */
    public static String ask(Component parent, String username) {
        JPasswordField pw = new JPasswordField(18);
        JPasswordField confirm = new JPasswordField(18);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("New password for " + username + ":"));
        p.add(pw);
        p.add(new JLabel("Confirm new password:"));
        p.add(confirm);

        int ok = JOptionPane.showConfirmDialog(parent, p, "Reset Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return null;

        char[] a = pw.getPassword();
        char[] b = confirm.getPassword();
        try {
            if (a.length == 0) return null;
            if (!Arrays.equals(a, b)) {
                JOptionPane.showMessageDialog(parent, "The two passwords do not match.",
                        "Reset Password", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return new String(a);
        } finally {
            Arrays.fill(a, ' ');
            Arrays.fill(b, ' ');
        }
    }
}
