package ui.Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 美化的确认对话框
 * 风格与 ModernWinDialog 保持一致
 */
public class ModernConfirmDialog extends JDialog {
    private boolean confirmed = false;

    public ModernConfirmDialog(JFrame parent, String title, String message) {
        super(parent, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        Color cardColor = new Color(30, 36, 55, 245);
        Color textPrimary = new Color(245, 247, 250);
        Color textSecondary = new Color(160, 170, 185);
        Color accentColor = new Color(66, 165, 245);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(cardColor);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2d.setColor(new Color(255, 255, 255, 35));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        titleLabel.setForeground(textPrimary);

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        messageLabel.setForeground(textSecondary);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(titleLabel, BorderLayout.NORTH);
        center.add(messageLabel, BorderLayout.CENTER);

        JButton yesBtn = new JButton("是");
        JButton noBtn = new JButton("否");
        styleButton(yesBtn, new Color(46, 204, 113), new Color(39, 174, 96));
        styleButton(noBtn, new Color(149, 165, 166), new Color(127, 140, 141));

        yesBtn.addActionListener(e -> { confirmed = true; dispose(); });
        noBtn.addActionListener(e -> { confirmed = false; dispose(); });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(yesBtn);
        buttonPanel.add(noBtn);

        mainPanel.add(center, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        setSize(360, 200);
        setLocationRelativeTo(parent);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void styleButton(JButton button, Color normalColor, Color hoverColor) {
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(normalColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 25, 8, 25));

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(hoverColor); button.repaint(); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(normalColor); button.repaint(); }
        });

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(c.getBackground());
                g2d.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 8, 8));
                g2d.dispose();
                super.paint(g, c);
            }
        });
    }
}
