package ui.Dialog;

import   Controller.GameController;
import ui.StartGameGUI;
import   utils.GameConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class GameSetupDialog extends JDialog {
    private static final Color CARD_COLOR = new Color(30, 36, 55, 245);
    private static final Color TEXT_PRIMARY = new Color(245, 247, 250);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 185);
    private static final Color ACCENT_COLOR = new Color(66, 165, 245); // 高亮天蓝色
    private static final int DIALOG_WIDTH = 540;
    private static final int DIALOG_HEIGHT = 440;

    private final GameController controller;
    private final GameConstants.GameMode mode;
    private final GameConstants.AIEngine selectedEngine;  // 新增：选定的AI引擎
    private JComboBox<GameConstants.AIDifficulty> difficultyBox;
    private final StartGameGUI startPage;

    public GameSetupDialog(JFrame parent, GameController controller, GameConstants.GameMode mode, GameConstants.AIEngine selectedEngine) {
        super(parent, true);
        this.startPage = (StartGameGUI) parent;
        this.controller = controller;
        this.mode = mode;
        this.selectedEngine = selectedEngine;  // 保存选定的引擎
        initUI();
    }

    private void initUI() {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setLocationRelativeTo(getParent());

        JPanel root = new TransparentPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel card = createCard();
        root.add(card, BorderLayout.CENTER);
        add(root);
    }

    private JPanel createCard() {
        JPanel card = new TransparentPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 22, 22);
                g2.dispose();
            }
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridy = 0;
        gbc.insets = new Insets(25, 36, 15, 36);
        card.add(createTitle(), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(10, 36, 15, 36);
        card.add(createFirstMoveButtons(), gbc);

        if (mode == GameConstants.GameMode.VS_AI && selectedEngine == GameConstants.AIEngine.MINIMAX) {
            gbc.gridy = 2;
            gbc.insets = new Insets(6, 88, 16, 88);
            card.add(createDifficultyPanel(), gbc);
        }

        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 20, 0);
        gbc.fill = GridBagConstraints.NONE;
        card.add(createCloseButton(), gbc);
        return card;
    }

    private JPanel createTitle() {
        JPanel panel = new TransparentPanel(new BorderLayout(0, 8));
        JLabel title = new JLabel("选择先手", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);

        String subtitle = mode == GameConstants.GameMode.VS_AI
                ? "选择执黑方，并设置电脑难度"
                : "选择哪一方执黑先行";
        JLabel sub = new JLabel(subtitle, SwingConstants.CENTER);
        sub.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECONDARY);

        panel.add(title, BorderLayout.CENTER);
        panel.add(sub, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFirstMoveButtons() {
        JPanel panel = new TransparentPanel(new GridLayout(1, 2, 30, 0));
        String blackText = mode == GameConstants.GameMode.VS_AI ? "玩家执黑先手" : "玩家一执黑先手";
        String whiteText = mode == GameConstants.GameMode.VS_AI ? "电脑执黑先手" : "玩家二执黑先手";

        panel.add(createChessButton(blackText, (byte) 1, true));
        panel.add(createChessButton(whiteText, (byte) 2, false));
        return panel;
    }

    /**
     * 精致绘制核心：高亮图形化棋子选择按钮
     */
    private JButton createChessButton(String text, byte firstMovePlayer, boolean isBlackChess) {
        JButton button = new JButton(text) {
            private boolean isHovered = false;

            {
                // 用监听器捕获鼠标悬停动作，实时刷新触发高亮
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int chessSize = 90; // 棋子圆盘大小
                int chessX = (w - chessSize) / 2;
                int chessY = 20;    // 棋子靠上留白

                // 1. 绘制鼠标悬停时的外围发光框 (Glow Effect)
                if (isHovered) {
                    // 外层淡蓝色光晕环
                    g2d.setColor(new Color(66, 165, 245, 30));
                    g2d.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 16, 16));
                    // 内层高亮拟合细线框
                    g2d.setColor(ACCENT_COLOR);
                    g2d.setStroke(new BasicStroke(2.2f));
                    g2d.draw(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 14, 14));
                } else {
                    // 默认状态下隐隐约约的暗色内衬卡片背景
                    g2d.setColor(new Color(255, 255, 255, 8));
                    g2d.fill(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 14, 14));
                }

                // 2. 绘制立体感棋子立体阴影
                g2d.setColor(new Color(0, 0, 0, isBlackChess ? 70 : 45));
                g2d.fill(new Ellipse2D.Float(chessX + 2, chessY + 4, chessSize, chessSize));

                // 3. 绘制棋子本体
                if (isBlackChess) {
                    // 渐变黑子（打造温润玉石质感）
                    GradientPaint gp = new GradientPaint(
                            chessX, chessY, new Color(55, 55, 62),
                            chessX, chessY + chessSize, new Color(15, 15, 18)
                    );
                    g2d.setPaint(gp);
                    g2d.fill(new Ellipse2D.Float(chessX, chessY, chessSize, chessSize));
                    // 边缘微高光
                    g2d.setColor(new Color(255, 255, 255, 25));
                    g2d.setStroke(new BasicStroke(1.2f));
                    g2d.draw(new Ellipse2D.Float(chessX, chessY, chessSize, chessSize));
                } else {
                    // 渐变白子
                    GradientPaint gp = new GradientPaint(
                            chessX, chessY, new Color(255, 255, 255),
                            chessX, chessY + chessSize, new Color(225, 228, 235)
                    );
                    g2d.setPaint(gp);
                    g2d.fill(new Ellipse2D.Float(chessX, chessY, chessSize, chessSize));
                    // 灰色优雅描边
                    g2d.setColor(new Color(175, 180, 190));
                    g2d.setStroke(new BasicStroke(1.2f));
                    g2d.draw(new Ellipse2D.Float(chessX, chessY, chessSize, chessSize));
                }

                // 4. 绘制下方的居中说明文字
                g2d.setFont(getFont());
                g2d.setColor(isHovered ? ACCENT_COLOR : TEXT_PRIMARY);
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (w - fm.stringWidth(getText())) / 2;
                int textY = h - 22; // 文字距离底部的边距
                g2d.drawString(getText(), textX, textY);

                g2d.dispose();
            }
        };

        button.setPreferredSize(new Dimension(200, 165)); // 扩宽增高，形成大卡片布局
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false); // 必须关闭 Swing 默认底色填充
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            dispose();
            GameConstants.AIDifficulty difficulty = difficultyBox == null
                    ? GameConstants.AIDifficulty.NORMAL
                    : (GameConstants.AIDifficulty) difficultyBox.getSelectedItem();
            // 同时关闭开始页面
            startPage.setVisible(false);
            SwingUtilities.invokeLater(() -> controller.Start(mode, firstMovePlayer, difficulty, selectedEngine));
        });
        return button;
    }

    private JPanel createDifficultyPanel() {
        JPanel panel = new TransparentPanel(new BorderLayout(12, 0));
        JLabel label = new JLabel("AI 难度");
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));

        difficultyBox = new JComboBox<>(GameConstants.AIDifficulty.values());
        difficultyBox.setSelectedItem(GameConstants.AIDifficulty.NORMAL);
        difficultyBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        difficultyBox.setBackground(new Color(245, 247, 250));
        difficultyBox.setForeground(new Color(30, 36, 55));

        panel.add(label, BorderLayout.WEST);
        panel.add(difficultyBox, BorderLayout.CENTER);
        return panel;
    }

    private JButton createCloseButton() {
        JButton close = new JButton("取消");
        close.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        close.setForeground(new Color(255, 115, 115));
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                // 取消时，开始页面保持显示
            }
        });
        return close;
    }

    private static class TransparentPanel extends JPanel {
        TransparentPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
    }
}
