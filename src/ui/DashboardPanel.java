package ui;

import dao.DashboardDAO;
import util.UITheme;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

/** Dashboard: system name, welcome line and summary cards. */
public class DashboardPanel extends JPanel {
    private final DashboardDAO dao = new DashboardDAO();
    private final JPanel cards = new JPanel(new GridLayout(0, 3, 12, 12));

    public DashboardPanel(String welcomeText) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(UITheme.BG_DARK);
        cards.setOpaque(false);

        JLabel title = new JLabel("Restro Rentals — Vehicle Rental System");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_LIGHT);
        JLabel welcome = new JLabel(welcomeText);
        welcome.setForeground(UITheme.TEXT_MUTED);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> load());

        JPanel north = new JPanel(new GridLayout(0, 1));
        north.setOpaque(false);
        north.add(title);
        north.add(welcome);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(north, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        load();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) { load(); }
        });
    }

    private void load() {
        cards.removeAll();
        for (Map.Entry<String, String> e : dao.summary().entrySet()) {
            JPanel card = new JPanel(new GridLayout(2, 1));
            card.setBackground(UITheme.BG_PANEL);
            card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            JLabel name = new JLabel(e.getKey());
            name.setForeground(UITheme.TEXT_MUTED);
            JLabel value = new JLabel(e.getValue());
            value.setFont(UITheme.FONT_TITLE);
            value.setForeground(UITheme.GOLD);
            card.add(name);
            card.add(value);
            cards.add(card);
        }
        cards.revalidate();
        cards.repaint();
    }
}
