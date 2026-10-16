package ui;

import util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Read-only date field that opens a reusable month-calendar dialog. */
public final class DatePickerField extends JPanel {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDate date;
    private final JLabel dateLabel = new JLabel();

    public DatePickerField(LocalDate initialDate) {
        super(new BorderLayout());
        setOpaque(false);

        JPanel field = new JPanel(new BorderLayout(8, 0));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 170, 195)),
            new EmptyBorder(4, 8, 4, 6)
        ));

        dateLabel.setForeground(UITheme.BG_DARK);
        dateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dateLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { openCalendar(); }
        });
        JButton calendarButton = new JButton(new CalendarIcon());
        calendarButton.setToolTipText("Choose date from calendar");
        calendarButton.getAccessibleContext().setAccessibleName("Open calendar");
        calendarButton.setBackground(Color.WHITE);
        calendarButton.setFocusPainted(false);
        calendarButton.setBorderPainted(false);
        calendarButton.setContentAreaFilled(false);
        calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarButton.addActionListener(e -> openCalendar());

        field.add(dateLabel, BorderLayout.CENTER);
        field.add(calendarButton, BorderLayout.EAST);
        add(field, BorderLayout.CENTER);
        setDate(initialDate);
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        boolean changed = !java.util.Objects.equals(date, this.date);
        this.date = date;
        dateLabel.setText(date == null ? "Choose date" : FORMAT.format(date));
        if (changed) {
            for (ActionListener listener : listenerList.getListeners(ActionListener.class)) {
                listener.actionPerformed(new java.awt.event.ActionEvent(this,
                    java.awt.event.ActionEvent.ACTION_PERFORMED, "dateChanged"));
            }
        }
    }

    public void addDateChangeListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    private static final class CalendarIcon implements Icon {
        private static final int WIDTH = 16;
        private static final int HEIGHT = 16;

        @Override public int getIconWidth() { return WIDTH; }
        @Override public int getIconHeight() { return HEIGHT; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UITheme.BG_DARK);
            g.drawRoundRect(x + 1, y + 3, 13, 11, 2, 2);
            g.fillRect(x + 1, y + 5, 14, 3);
            g.drawLine(x + 4, y + 1, x + 4, y + 5);
            g.drawLine(x + 11, y + 1, x + 11, y + 5);
            g.fillRect(x + 4, y + 9, 2, 2);
            g.fillRect(x + 8, y + 9, 2, 2);
            g.fillRect(x + 4, y + 12, 2, 1);
            g.fillRect(x + 8, y + 12, 2, 1);
            g.dispose();
        }
    }

    private void openCalendar() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        DatePickerDialog dialog = new DatePickerDialog(owner, date);
        dialog.setVisible(true);
        if (dialog.getSelectedDate() != null) setDate(dialog.getSelectedDate());
    }

    private static final class DatePickerDialog extends JDialog {
        private LocalDate selectedDate;
        private YearMonth displayedMonth;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        private final Color selectedColor = new Color(8, 45, 110);

        DatePickerDialog(Window owner, LocalDate initialDate) {
            super(owner, "Choose Date", ModalityType.APPLICATION_MODAL);
            selectedDate = initialDate;
            displayedMonth = YearMonth.from(initialDate == null ? LocalDate.now() : initialDate);

            JPanel root = new JPanel(new BorderLayout(8, 10));
            root.setBorder(new EmptyBorder(12, 12, 12, 12));
            root.setBackground(UITheme.BG_DARK);

            JButton previous = navigationButton("\u2039");
            JButton next = navigationButton("\u203A");
            previous.addActionListener(e -> {
                displayedMonth = displayedMonth.minusMonths(1);
                renderCalendar();
            });
            next.addActionListener(e -> {
                displayedMonth = displayedMonth.plusMonths(1);
                renderCalendar();
            });
            monthLabel.setForeground(UITheme.TEXT_LIGHT);
            monthLabel.setFont(UITheme.FONT_HEADER);
            JPanel heading = new JPanel(new BorderLayout(8, 0));
            heading.setOpaque(false);
            heading.add(previous, BorderLayout.WEST);
            heading.add(monthLabel, BorderLayout.CENTER);
            heading.add(next, BorderLayout.EAST);

            daysPanel.setOpaque(false);
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            footer.setOpaque(false);
            JButton today = UITheme.primaryButton("Today");
            JButton cancel = new JButton("Cancel");
            today.addActionListener(e -> chooseDate(LocalDate.now()));
            cancel.addActionListener(e -> dispose());
            footer.add(today);
            footer.add(cancel);

            root.add(heading, BorderLayout.NORTH);
            root.add(daysPanel, BorderLayout.CENTER);
            root.add(footer, BorderLayout.SOUTH);
            setContentPane(root);
            renderCalendar();
            pack();
            setResizable(false);
            setLocationRelativeTo(owner);
        }

        LocalDate getSelectedDate() {
            return selectedDate;
        }

        private void renderCalendar() {
            monthLabel.setText(displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + displayedMonth.getYear());
            daysPanel.removeAll();

            for (DayOfWeek day : DayOfWeek.values()) {
                JLabel label = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
                label.setForeground(UITheme.TEXT_MUTED);
                label.setFont(UITheme.FONT_BODY.deriveFont(Font.BOLD, 13f));
                daysPanel.add(label);
            }

            int offset = displayedMonth.atDay(1).getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) daysPanel.add(new JLabel());
            for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
                LocalDate candidate = displayedMonth.atDay(day);
                JButton button = new JButton(Integer.toString(day));
                button.setFocusPainted(false);
                button.setOpaque(true);
                button.setBackground(candidate.equals(selectedDate) ? selectedColor : Color.WHITE);
                button.setForeground(candidate.equals(selectedDate) ? Color.WHITE : UITheme.BG_DARK);
                button.setBorder(BorderFactory.createLineBorder(
                    candidate.equals(LocalDate.now()) ? UITheme.RED_ACCENT : new Color(205, 215, 230)));
                button.addActionListener(e -> chooseDate(candidate));
                daysPanel.add(button);
            }
            daysPanel.revalidate();
            daysPanel.repaint();
            pack();
        }

        private void chooseDate(LocalDate date) {
            selectedDate = date;
            dispose();
        }

        private JButton navigationButton(String text) {
            JButton button = new JButton(text);
            button.setForeground(UITheme.TEXT_LIGHT);
            button.setBackground(UITheme.BG_PANEL);
            button.setFocusPainted(false);
            return button;
        }
    }
}
