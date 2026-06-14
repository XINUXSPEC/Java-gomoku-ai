package ui;

import   Controller.GameController;
import ui.Dialog.AIEngineSelectDialog;
import ui.Dialog.GameSetupDialog;
import   utils.GameConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class StartGameGUI extends JFrame {

    private GridBagConstraints gbc;
    JButton vsPlayerButton, vsAiButton;
    JPanel backgroundPanel;
    /** 控制器引用（用于程序退出时销毁AI） */
    private Controller.GameController controller;

    /** 防止重复触发关闭流程 */
    private volatile boolean shuttingDown = false;

    public StartGameGUI() {
        //窗口初始化
        setTitle("五子棋 - 极简现代版");
        setSize(GameConstants.WindowWidth, GameConstants.WindowHeight);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 关闭窗口：UI 线程只负责禁用界面，耗时的等待+销毁放到后台线程
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (shuttingDown) {
                    return;
                }
                shuttingDown = true;
                setEnabled(false);
                setVisible(false);
                setTitle("五子棋 - 正在退出...");

                new Thread(() -> {
                    if (controller != null) {
                        controller.shutdownAI();
                    }
                    System.exit(0);
                }, "App-Shutdown-Thread").start();
            }
        });

        //UI
        BackgroundPanelAndInit();
        CreateAndAddTitle();
        CreateAndAddButton();
    }

    //按键事件绑定
    public void AddButtonListener(GameController controller){
        this.controller = controller;  // 🎯 保存引用，关闭窗口时使用
        vsPlayerButton.addActionListener(e -> {
            new GameSetupDialog(this, controller, GameConstants.GameMode.VS_PLAYER, null).setVisible(true);
        });

        vsAiButton.addActionListener(e -> {
            // 使用数组来解决"向前引用"问题
            final AIEngineSelectDialog[] dialogHolder = new AIEngineSelectDialog[1];
            
            dialogHolder[0] = new AIEngineSelectDialog(this, () -> {
                GameConstants.AIEngine selectedEngine = dialogHolder[0].getSelectedEngine();
                if (selectedEngine != null) {
                    new GameSetupDialog(this, controller, GameConstants.GameMode.VS_AI, selectedEngine).setVisible(true);
                }
            });
            dialogHolder[0].setVisible(true);
        });
    }

    //界面初始化
    private void BackgroundPanelAndInit(){
        backgroundPanel= new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 从深灰到微蓝灰的渐变
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 35, 45),
                        0, getHeight(), new Color(45, 50, 65));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        add(backgroundPanel);
    }

    // 艺术字标题标签
    private void CreateAndAddTitle(){
        JLabel titleLabel = new JLabel("五 子 棋");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 75));
        titleLabel.setForeground(new Color(240, 240, 245));
        // 添加文本发光/阴影的艺术效果
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 80, 0);
        backgroundPanel.add(titleLabel, gbc);
    }

    // 创建现代交互模式按钮
    private void CreateAndAddButton(){
        vsPlayerButton = new JButton("双人对战");
        styleModernButton(vsPlayerButton, new Color(70, 130, 180), new Color(100, 149, 237));
        vsAiButton = new JButton("人机对战");
        styleModernButton(vsAiButton, new Color(46, 139, 87), new Color(60, 179, 113));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 0));
        buttonPanel.add(vsPlayerButton);
        buttonPanel.add(vsAiButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        backgroundPanel.add(buttonPanel, gbc);
    }

    // 现代圆角呼吸按钮样式封装
    private void styleModernButton(JButton button, Color normalColor, Color hoverColor) {
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        button.setForeground(Color.WHITE);
        button.setBackground(normalColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false); // 关键：禁用默认渲染以支持自定义圆角
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 鼠标变成手型
        button.setBorder(BorderFactory.createEmptyBorder(15, 45, 15, 45));

        // 注入高动态交互事件
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normalColor);
                button.repaint();
            }
        });

        // 拦截绘制，渲染出极其高级的圆角和渐变阴影
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 绘制柔和圆角矩形背景
                g2d.setColor(c.getBackground());
                g2d.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 20, 20));
                g2d.dispose();
                super.paint(g, c);
            }
        });
    }
}