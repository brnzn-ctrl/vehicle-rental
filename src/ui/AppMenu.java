package ui;

import javax.swing.*;
import java.awt.event.KeyEvent;

/** Menu bar pieces and the Log Out / Exit actions shared by the staff window and the customer portal. */
public final class AppMenu {
    private AppMenu() { }

    /** Asks first, then closes this window and shows the login screen again (ends the session). */
    public static void logout(JFrame frame) {
        int ok = JOptionPane.showConfirmDialog(frame, "Log out and return to the login screen?",
                "Log Out", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            new LoginForm().setVisible(true);
            frame.dispose();
        }
    }

    /** Asks first, then ends the program (the shutdown hook closes Derby cleanly). */
    public static void exit(JFrame frame) {
        int ok = JOptionPane.showConfirmDialog(frame, "Exit the application?", "Exit", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) System.exit(0);
    }

    /** File menu: Log Out (Ctrl+L) and Exit (Ctrl+Q). */
    public static JMenu fileMenu(JFrame frame) {
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem logout = new JMenuItem("Log Out");
        logout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        logout.addActionListener(e -> logout(frame));
        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        exit.addActionListener(e -> exit(frame));
        file.add(logout);
        file.addSeparator();
        file.add(exit);
        return file;
    }

    /** Help menu with a simple About box. */
    public static JMenu helpMenu(JFrame frame) {
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "Restro Rentals\nVehicle Rental Management System\nJava Swing / AWT, JDBC, Apache Derby",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        return help;
    }
}
