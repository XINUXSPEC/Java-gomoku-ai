package ui.Dialog;

import ui.StartGameGUI;
import utils.GameConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * AI引擎选择对话框
 * 在启动人机对战前，让用户选择使用Minimax或AlphaZero引擎
 */
public class AIEngineSelectDialog extends JDialog {
    private static final Color CARD_COLOR = new Color(30, 36, 55, 245);
    private static final Color TEXT_PRIMARY = new Color(245, 247, 250);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 185);
    private static final Color ACCENT_COLOR_1 = new Color(100, 149, 237); // 蓝色（Minimax）
    private static final Color ACCENT_COLOR_2 = new Color(60, 179, 113);  // 绿色（AlphaZero）
    private static final int DIALOG_WIDTH = 560;
    private static final int DIALOG_HEIGHT = 420;

    private GameConstants.AIEngine selectedEngine = null;
    private final Runnable onEngineSelected;
    private final StartGameGUI startPage;

    public AIEngineSelectDialog(JFrame parent, Runnable onEngineSelected) {
        super(parent, true);
        this.startPage = (StartGameGUI) parent;
        this.onEngineSelected = onEngineSelected;
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

        // 标题
        gbc.gridy = 0;
        gbc.insets = new Insets(25, 36, 15, 36);
        card.add(createTitle(), gbc);

        // 引擎选择按钮
        gbc.gridy = 1;
        gbc.insets = new Insets(15, 36, 15, 36);
        card.add(createEngineSelectionPanel(), gbc);

        // 关闭按钮
        gbc.gridy = 2;
        gbc.insets = new Insets(8, 0, 20, 0);
        gbc.fill = GridBagConstraints.NONE;
        card.add(createCloseButton(), gbc);

        return card;
    }

    private JPanel createTitle() {
        JPanel panel = new TransparentPanel(new BorderLayout(0, 8));
        JLabel title = new JLabel("选择 AI 引擎", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel("选择你要对战的 AI 对手风格", SwingConstants.CENTER);
        sub.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECONDARY);

        panel.add(title, BorderLayout.CENTER);
        panel.add(sub, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createEngineSelectionPanel() {
        JPanel panel = new TransparentPanel(new GridLayout(1, 2, 20, 0));
        panel.add(createEngineButton(
                GameConstants.AIEngine.MINIMAX,
                "Minimax",
                "速度快，落子即时\n棋力一般，休闲娱乐",
                ACCENT_COLOR_1
        ));
        panel.add(createEngineButton(
                GameConstants.AIEngine.ALPHA_ZERO,
                "AlphaZero",
                "棋力很强，深度学习\n速度一般，建议专业玩家",
                ACCENT_COLOR_2
        ));
        return panel;
    }

    private JButton createEngineButton(GameConstants.AIEngine engine, String name, String desc, Color accentColor) {
        JButton button = new JButton() {
            private boolean isHovered = false;

            {
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

                addActionListener(e -> {
                    selectedEngine = engine;
                    dispose();
                    if (onEngineSelected != null) {
                        onEngineSelected.run();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 背景卡片
                if (isHovered) {
                    g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
                    g2d.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 16, 16));
                    g2d.setColor(accentColor);
                    g2d.setStroke(new BasicStroke(2.2f));
                    g2d.draw(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 14, 14));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 8));
                    g2d.fill(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 14, 14));
                }

                // 绘制文本
                g2d.setColor(accentColor);
                g2d.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int nameX = (w - fm.stringWidth(name)) / 2;
                g2d.drawString(name, nameX, 50);

                // 绘制描述文本
                g2d.setColor(TEXT_SECONDARY);
                g2d.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
                String[] lines = desc.split("\n");
                int descStartY = 85;
                for (String line : lines) {
                    fm = g2d.getFontMetrics();
                    int lineX = (w - fm.stringWidth(line)) / 2;
                    g2d.drawString(line, lineX, descStartY);
                    descStartY += 18;
                }

                g2d.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 150);
            }
        };

        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        return button;
    }

    private JPanel createCloseButton() {
        JPanel panel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeBtn = new JButton("取消") {
            private boolean isHovered = false;

            {
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

                addActionListener(e -> {
                    selectedEngine = null;
                    dispose();
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (isHovered) {
                    g2d.setColor(new Color(100, 100, 110, 30));
                    g2d.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
                    g2d.setColor(new Color(180, 180, 190));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.draw(new RoundRectangle2D.Float(4, 4, w - 8, h - 8, 8, 8));
                }

                super.paintComponent(g);
                g2d.dispose();
            }
        };

        closeBtn.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        closeBtn.setForeground(TEXT_SECONDARY);
        closeBtn.setOpaque(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(100, 36));

        panel.add(closeBtn);
        return panel;
    }

    public GameConstants.AIEngine getSelectedEngine() {
        return selectedEngine;
    }

    /**
     * 透明面板基类
     */
    private static class TransparentPanel extends JPanel {
        public TransparentPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
    }
}
