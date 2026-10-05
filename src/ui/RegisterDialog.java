package ui;

import util.CustomerTheme;
import util.PasswordHasher;
import util.Validator;
import dao.CustomerDAO;
import model.Customer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Customer self-registration. Enforces unique username with a real DB check before insert. */
public class RegisterDialog extends JDialog {

    public String registeredUsername; // set on success, so LoginForm can prefill it

    private final CustomerDAO customerDAO;
    private final JTextField txtUsername = new BoxField("Choose a username");
    private final JPasswordField txtPassword = new BoxPassword("At least 6 characters");
    private final JPasswordField txtConfirm = new BoxPassword("Re-enter your password");
    private final JTextField txtFirstName = new BoxField("First name");
    private final JTextField txtLastName = new BoxField("Last name");
    private final JTextField txtEmail = new BoxField("name@example.com");
    private final JTextField txtPhone = new BoxField("11-digit mobile number");
    private final JTextField txtLicense = new BoxField("License number");

    public RegisterDialog(Frame owner, CustomerDAO customerDAO) {
        super(owner, "Create Customer Account", true);
        this.customerDAO = customerDAO;
        setResizable(false);

        // Gradient page background, same look as the customer portal
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = CustomerTheme.smooth(g);
                g2.setPaint(new GradientPaint(0, 0, CustomerTheme.BG_TOP, 0, getHeight(), CustomerTheme.BG_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new RadialGradientPaint(new Point(getWidth() - 60, -30), Math.max(getWidth(), getHeight()) * 0.7f,
                    new float[]{0f, 1f}, new Color[]{new Color(212, 175, 55, 45), new Color(212, 175, 55, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(22, 28, 24, 28));
        setContentPane(root);

        // ---- Header ----
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("RESTRO RENTALS");
        brand.setFont(CustomerTheme.bold(13));
        brand.setForeground(CustomerTheme.GOLD);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Create Your Account");
        title.setFont(CustomerTheme.script(30));
        title.setForeground(CustomerTheme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Join us to reserve your next ride");
        sub.setFont(CustomerTheme.body(14));
        sub.setForeground(CustomerTheme.MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        JSeparator gold = new JSeparator() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = CustomerTheme.smooth(g);
                g2.setColor(CustomerTheme.GOLD);
                g2.fillRoundRect((getWidth() - 60) / 2, 0, 60, 3, 3, 3);
                g2.dispose();
            }
        };
        gold.setPreferredSize(new Dimension(60, 3));
        gold.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        header.add(brand);
        header.add(Box.createVerticalStrut(2));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(sub);
        header.add(Box.createVerticalStrut(10));
        header.add(gold);
        header.add(Box.createVerticalStrut(14));
        root.add(header, BorderLayout.NORTH);

        // ---- White card with the form ----
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = CustomerTheme.smooth(g);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4, 24, 24);   // soft shadow
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 24, 24);
                g2.setColor(CustomerTheme.CARD_EDGE);
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 0, 0);

        int r = 0;
        addSection(card, gc, r++, "ACCOUNT");
        addField(card, gc, r++, "Username", txtUsername);
        addField(card, gc, r++, "Password", txtPassword);
        addField(card, gc, r++, "Confirm Password", txtConfirm);
        addSection(card, gc, r++, "PERSONAL DETAILS");
        addTwoFields(card, gc, r++, "First Name", txtFirstName, "Last Name", txtLastName);
        addField(card, gc, r++, "Email", txtEmail);
        addField(card, gc, r++, "Phone (11 digits)", txtPhone);
        addField(card, gc, r++, "Driver's License No.", txtLicense);
        root.add(card, BorderLayout.CENTER);

        // ---- Buttons ----
        JButton btnCreate = CustomerTheme.pill("Create Account", true);
        JButton btnCancel = CustomerTheme.pill("Cancel", false);
        btnCreate.addActionListener(e -> attemptRegister());
        btnCancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        buttons.add(btnCreate);
        buttons.add(btnCancel);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnCreate);   // Enter submits

        pack();   // size from content, so it always fits whatever the font scale is
        setLocationRelativeTo(owner);
    }

    private static void addSection(JPanel card, GridBagConstraints gc, int row, String text) {
        JLabel l = new JLabel(text);
        l.setFont(CustomerTheme.bold(12));
        l.setForeground(CustomerTheme.GOLD);
        gc.gridy = row; gc.gridwidth = 2;
        gc.insets = new Insets(row == 0 ? 0 : 10, 0, 6, 0);
        card.add(l, gc);
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(CustomerTheme.bold(13));
        l.setForeground(CustomerTheme.TEXT);
        return l;
    }

    /** Label above its field, full width. */
    private static void addField(JPanel card, GridBagConstraints gc, int row, String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(fieldLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        gc.gridy = row; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 10, 0);
        card.add(p, gc);
    }

    /** Two labelled fields side by side (first / last name). */
    private static void addTwoFields(JPanel card, GridBagConstraints gc, int row,
                                     String l1, JComponent f1, String l2, JComponent f2) {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        for (Object[] x : new Object[][]{{l1, f1}, {l2, f2}}) {
            JPanel c = new JPanel(new BorderLayout(0, 4));
            c.setOpaque(false);
            c.add(fieldLabel((String) x[0]), BorderLayout.NORTH);
            c.add((JComponent) x[1], BorderLayout.CENTER);
            p.add(c);
        }
        gc.gridy = row; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 10, 0);
        card.add(p, gc);
    }

    // ---- Visible rounded input boxes (clear border, gold glow on focus, grey hint while empty) ----

    private static void paintBox(JTextField f, Graphics g, String hint) {
        Graphics2D g2 = CustomerTheme.smooth(g);
        int w = f.getWidth(), h = f.getHeight();
        boolean focus = f.hasFocus();
        g2.setColor(focus ? new Color(255, 251, 238) : new Color(246, 247, 251));
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        if (focus) {
            g2.setColor(new Color(212, 175, 55, 70));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);   // soft glow
        }
        g2.setStroke(new BasicStroke(focus ? 2f : 1.4f));
        g2.setColor(focus ? CustomerTheme.GOLD : new Color(120, 126, 145));
        g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
        if (f.getDocument().getLength() == 0 && !focus) {
            g2.setFont(CustomerTheme.italic(14));
            g2.setColor(new Color(150, 154, 168));
            Insets in = f.getInsets();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, in.left, (h + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
    }

    private static void styleBox(JTextField f) {
        f.setOpaque(false);
        f.setFont(CustomerTheme.body(16));
        f.setForeground(CustomerTheme.TEXT);
        f.setCaretColor(CustomerTheme.GOLD);
        f.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        f.setPreferredSize(new Dimension(300, 42));   // fixed, so it can never collapse
        f.setMinimumSize(new Dimension(120, 42));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.repaint(); }
            @Override public void focusLost(FocusEvent e) { f.repaint(); }
        });
    }

    private static final class BoxField extends JTextField {
        private final String hint;
        BoxField(String hint) { this.hint = hint; styleBox(this); }
        @Override protected void paintComponent(Graphics g) { paintBox(this, g, hint); super.paintComponent(g); }
    }

    private static final class BoxPassword extends JPasswordField {
        private final String hint;
        BoxPassword(String hint) { this.hint = hint; styleBox(this); }
        @Override protected void paintComponent(Graphics g) { paintBox(this, g, hint); super.paintComponent(g); }
    }

    private void attemptRegister() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirm  = new String(txtConfirm.getPassword());
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String license = txtLicense.getText().trim();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, password, first and last name are required.");
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.");
            return;
        }

        if (!Validator.isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address (e.g. name@example.com).");
            return;
        }
        if (!Validator.isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 11 digits.");
            return;
        }

        // Systematic uniqueness check — this is what stops two different people
        // from registering the same username.
        if (customerDAO.usernameExists(username)) {
            JOptionPane.showMessageDialog(this,
                    "That username is already taken. Please choose another.",
                    "Username Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Customer c = new Customer();
        c.setUsername(username);
        c.setPasswordHash(PasswordHasher.hash(password));
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email.isEmpty() ? null : email);
        c.setPhone(phone.isEmpty() ? null : phone);
        c.setLicenseNumber(license.isEmpty() ? null : license);
        c.setActive(true);

        try {
            customerDAO.insert(c);
            registeredUsername = username;
            JOptionPane.showMessageDialog(this, "Account created! You can now log in.");
            dispose();
        } catch (RuntimeException ex) {
            // Covers the rare race condition where two people submit the same
            // username at nearly the same instant — the DB's UNIQUE constraint
            // catches what the earlier check might miss.
            JOptionPane.showMessageDialog(this,
                    "Could not create account — that username or email/license may already be in use.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}