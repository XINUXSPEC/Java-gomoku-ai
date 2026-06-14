package ui.Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ModernWinDialog extends JDialog {
    private boolean isRestart = true; // 记录玩家的选择

    public ModernWinDialog(JFrame parent, String winnerName, Color winnerColor) {
        super(parent, true); // 设为模态对话框
        setUndecorated(true); // 去掉原生极其丑陋的边框和标题栏
        setBackground(new Color(0, 0, 0, 0)); // 设置窗体本身完全透明，交由自定义面板渲染圆角

        // 1. 主面板设计 (暗黑圆角卡片)
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 优雅的深灰卡片底色
                g2d.setColor(new Color(40, 40, 48));
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                // 顶部加一条精致的霓虹点缀边框
                g2d.setColor(winnerColor);
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawLine(0, 2, getWidth(), 2);
                g2d.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // 2. 胜利大标题
        JLabel titleLabel = new JLabel("斩 获 胜 利", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 3. 中间提示文本（动态展示谁赢了）
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        contentPanel.setOpaque(false);

        JLabel winText = new JLabel(winnerName);
        winText.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        winText.setForeground(new Color(230, 230, 235));

        JLabel subText = new JLabel("荣耀胜出！高手，要不要再来一局激烈的对决？");
        subText.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        subText.setForeground(new Color(170, 170, 180));

        contentPanel.add(winText);
        contentPanel.add(subText);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 4. 底部极简操控按钮（再来一局 vs 返回主页）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setOpaque(false);

        JButton btnRestart = new JButton("再来一局");
        JButton btnHome = new JButton("返回主页");

        // 撞色扁平化按钮样式
        styleDialogButton(btnRestart, new Color(46, 204, 113), new Color(39, 174, 96)); // 胜利绿
        styleDialogButton(btnHome, new Color(149, 165, 166), new Color(127, 140, 141));   // 高级灰

        btnRestart.addActionListener(e -> {
            isRestart = true;
            dispose();
        });

        btnHome.addActionListener(e -> {
            isRestart = false;
            dispose();
        });

        buttonPanel.add(btnRestart);
        buttonPanel.add(btnHome);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setSize(420, 240);
        setLocationRelativeTo(parent); // 相对主界面完美居中
    }

    // 辅助获取选择结果
    public boolean getChoice() {
        return isRestart;
    }

    // 扁平圆角按钮封装逻辑
    private void styleDialogButton(JButton button, Color normalColor, Color hoverColor) {
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(normalColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

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