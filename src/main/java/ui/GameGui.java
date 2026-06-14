package ui;

import Controller.GameController;
import logic.ChessInputListener;
import ui.Dialog.ModernConfirmDialog;
import ui.Dialog.ModernWinDialog;
import utils.GameConstants;
import utils.BoardUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class GameGui extends JFrame {
    private JPanel chessPanel;
    private BufferedImage bufferedImage;      // 主画布（离屏双缓冲）
    private BufferedImage staticBoardImage;  // 静态棋盘缓存（背景+网格+天元）
    private final Color backgroundColor = new Color(232, 200, 162);

    // 🎯 【底层连通】：挂载当前正在活跃的 Toast 提示图层句柄
    private ToastNotification activeToast = null;

    private List<Point> winningLine = null;
    private Timer animationTimer;
    private boolean flashState = false;
    private byte winnerColor = 0;
    private Point lastGhostPos = null;
    private byte lastGhostColor = 0;
    private Point lastHoverPoint = null;

    private JLabel leftPlayerName;
    private JLabel rightPlayerName;
    private JLabel scoreLabel;
    private JPanel topPanel;
    private byte currentActiveTurn = 1;

    private final GameConstants.GameMode gameMode;
    private byte firstMovePlayer;
    private final GameController gameController;

    public GameGui(GameController gameController, GameConstants.GameMode gameMode, byte firstMovePlayer) {
        this.gameController = gameController;
        this.gameMode = gameMode;
        this.firstMovePlayer = firstMovePlayer;
        initUI();
        initChessBoard();
        refreshBoard();
    }

    /**
     * 🎯 【失而复得】：重新加回来的首秀玩家设置函数
     */
    public void setFirstMovePlayer(byte player) {
        this.firstMovePlayer = player;
        if (topPanel != null) {
            topPanel.repaint();
        }
    }

    /**
     * 🎯 【核心中枢】：供外部逻辑（Controller 或监听器）直接调用的安全 Toast 唤醒入口
     */
    public void showToast(String message) {
        // 如果前一个 Toast 还在全力执行动画，直接拦截，防止高频点击造成 Timer 堆积
        if (activeToast != null && activeToast.isActive()) {
            return;
        }
        // 传入 chessPanel 作为驱动源，让动画的刷新能带得动棋盘重绘
        activeToast = new ToastNotification(chessPanel, message);
        activeToast.showNotification();
    }

    private void initUI() {
        setSize(GameConstants.WindowWidth, GameConstants.WindowHeight);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("五子棋 - GoBang Duel");
        setLayout(new BorderLayout());
        setResizable(false);
        setLocationRelativeTo(null);

        topPanel = createTopPanel();

        chessPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // 1. 彻底擦除屏幕旧像素，防止任何重影堆积

                Graphics2D g2d = (Graphics2D) g;
                // 🎯 全链路最高级别抗锯齿注入
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                // 🎯 步骤 A：永远第一步贴上“绝对静止、纯净”的静态棋盘底图
                if (staticBoardImage != null) {
                    g2d.drawImage(staticBoardImage, 0, 0, null);
                }

                // 🎯 步骤 B：现场实时绘制矩阵里的所有正式棋子（直接画在屏幕画笔上，不污染底图）
                byte[][] panel = gameController.getGameState().getPanel();
                for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
                    for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                        if (panel[i][j] == 1 || panel[i][j] == 2) {
                            // 传入当前屏幕的 g2d 直接绘制
                            drawPiece(g2d, panel[i][j], i, j, isWinningPoint(i, j));
                        }
                    }
                }
                drawLastMoveMarker(g2d); // 绘制落子标记

                // 🎯 步骤 C：如果当前鼠标有悬停的残影，现场叠加上去
                if (lastHoverPoint != null && gameController.isValidMove(lastHoverPoint.x, lastHoverPoint.y)) {
                    byte currentColor = gameController.getGameState().getCurrentPlayerColor();
                    Color ghostColor = currentColor == 1 ? GameConstants.PREVIEW_BLACK : GameConstants.PREVIEW_WHITE;
                    g2d.setColor(ghostColor);
                    g2d.fill(new Ellipse2D.Float(
                            BoardUtils.IndexTurnCoordinate(lastHoverPoint.x),
                            BoardUtils.IndexTurnCoordinate(lastHoverPoint.y),
                            GameConstants.ChessSize,
                            GameConstants.ChessSize
                    ));
                }

                // 🎯 步骤 D：最后，把临时的 Toast 动画层无污染地盖在最表面
                if (activeToast != null && activeToast.isActive()) {
                    activeToast.render(g2d, getWidth(), getHeight());
                }
            }
        };

        chessPanel.setBackground(backgroundColor);
        int boardWidth = GameConstants.WindowWidth - 16;
        int boardHeight = GameConstants.WindowHeight - GameConstants.PanelHeight * 2 - 32;
        chessPanel.setPreferredSize(new Dimension(boardWidth, boardHeight));

        JPanel bottomPanel = createBottomPanel();
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBackground(GameConstants.COLOR_BG_DARK);
        containerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        containerPanel.add(topPanel, BorderLayout.PAGE_START);
        containerPanel.add(chessPanel, BorderLayout.CENTER);
        containerPanel.add(bottomPanel, BorderLayout.PAGE_END);
        add(containerPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                outAndExit();
            }
        });

        setVisible(true);
    }

    private void initChessBoard() {
        int w = GameConstants.WindowWidth - 16;
        int h = GameConstants.WindowHeight - GameConstants.PanelHeight * 2 - 32;

        staticBoardImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        drawStaticBoard();
        bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        winningLine = null;
        flashState = false;
        lastGhostPos = null;
        lastGhostColor = 0;
        lastHoverPoint = null;
        lastDrawnPanel = null;
        activeToast = null; // 重置对局时清理 Toast 引用
    }

    private void drawStaticBoard() {
        if (staticBoardImage == null) return;
        Graphics2D g2 = staticBoardImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        try {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, staticBoardImage.getWidth(), staticBoardImage.getHeight());

            int pad = GameConstants.MainPadding;
            int size = GameConstants.ChessBorderSize;
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRect(pad - 4, pad - 4, size + 8, size + 8);

            g2.setColor(GameConstants.COLOR_GRID_LINE);
            g2.setStroke(new BasicStroke(1.0f));
            for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
                int offset = pad + i * GameConstants.ChessBoardSize;
                g2.drawLine(pad, offset, pad + size, offset);
                g2.drawLine(offset, pad, offset, pad + size);
            }

            g2.setColor(new Color(60, 45, 30));
            int r = GameConstants.TianYuanSize;
            int center = (GameConstants.ChessBorderNum - 1) / 2;
            int[][] stars = {{center, center}, {3, 3}, {3, 11}, {11, 3}, {11, 11}};
            for (int[] star : stars) {
                int sx = pad + star[0] * GameConstants.ChessBoardSize - r / 2;
                int sy = pad + star[1] * GameConstants.ChessBoardSize - r / 2;
                g2.fill(new Ellipse2D.Float(sx, sy, r, r));
            }
        } finally {
            g2.dispose();
        }
    }

    private byte[][] lastDrawnPanel = null;

    public void refreshBoard() {
        if (bufferedImage == null || staticBoardImage == null) {
            initChessBoard();
        }
        byte[][] panel = gameController.getGameState().getPanel();
        lastDrawnPanel = null;
        redrawFull(panel);
        lastDrawnPanel = copyPanel(panel);
        lastGhostPos = null;
        lastGhostColor = 0;
        lastHoverPoint = null;
        chessPanel.repaint();
    }

    public void drawAllPieces(byte[][] panel) {
        if (bufferedImage == null || staticBoardImage == null) {
            initChessBoard();
        }
        if (winningLine == null && lastDrawnPanel != null && panelsEqual(lastDrawnPanel, panel)) {
            return;
        }
        redrawFull(panel);
        lastDrawnPanel = copyPanel(panel);
        lastGhostPos = null;
        lastGhostColor = 0;
        lastHoverPoint = null;
        chessPanel.repaint();
    }

    private void redrawFull(byte[][] panel) {
        if (bufferedImage == null || staticBoardImage == null) return;

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // 1. 贴上完全无毛刺的静态棋盘背景
            g2.drawImage(staticBoardImage, 0, 0, null);

            // 2. 画出矩阵里所有的动态棋子
            for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
                for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                    if (panel[i][j] == 1 || panel[i][j] == 2) {
                        drawPiece(g2, panel[i][j], i, j, isWinningPoint(i, j));
                    }
                }
            }
            drawLastMoveMarker(g2);


        } finally {
            g2.dispose();
        }
    }

    private void drawPiece(Graphics2D g2, byte color, int x, int y, boolean isWinning) {
        int cx = BoardUtils.IndexTurnCoordinate(x);
        int cy = BoardUtils.IndexTurnCoordinate(y);
        int cs = GameConstants.ChessSize;

        if (isWinning && flashState) {
            Color flashColor = winnerColor == 1 ? new Color(46, 204, 113) : new Color(241, 196, 15);
            g2.setColor(flashColor);
            g2.fill(new Ellipse2D.Float(cx, cy, cs, cs));
            return;
        }

        g2.setColor(new Color(0, 0, 0, color == 1 ? 50 : 35));
        g2.fill(new Ellipse2D.Float(cx + 1, cy + 2, cs, cs));

        if (color == 1) {
            g2.setColor(GameConstants.COLOR_PIECE_BLACK);
            g2.fill(new Ellipse2D.Float(cx, cy, cs, cs));
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new Ellipse2D.Float(cx, cy, cs, cs));
        } else {
            g2.setColor(GameConstants.COLOR_PIECE_WHITE);
            g2.fill(new Ellipse2D.Float(cx, cy, cs, cs));
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new Ellipse2D.Float(cx, cy, cs, cs));
        }
    }

    private void drawLastMoveMarker(Graphics2D g2) {
        Point lastMove = gameController.getGameSystem().getLastMove();
        if (lastMove == null) return;

        int cx = BoardUtils.IndexTurnCoordinate(lastMove.x);
        int cy = BoardUtils.IndexTurnCoordinate(lastMove.y);
        int cs = GameConstants.ChessSize;

        int centerX = cx + cs / 2;
        int centerY = cy + cs / 2;

        int dotSize = 8;
        int dotX = centerX - dotSize / 2;
        int dotY = centerY - dotSize / 2;

        g2.setColor(new Color(231, 76, 60));
        g2.fill(new Ellipse2D.Float(dotX, dotY, dotSize, dotSize));

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fill(new Ellipse2D.Float(centerX - 1, centerY - 1, 2, 2));
    }

    public void drawChessImg(byte[][] panel, int ghostX, int ghostY, byte currentColor) {
        if (bufferedImage == null || staticBoardImage == null) return;
        if (lastGhostPos != null && lastGhostPos.x == ghostX && lastGhostPos.y == ghostY && lastGhostColor == currentColor) {
            return;
        }

        redrawFull(panel);

        Graphics2D g2 = bufferedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            Color ghostColor = currentColor == 1 ? GameConstants.PREVIEW_BLACK : GameConstants.PREVIEW_WHITE;
            g2.setColor(ghostColor);
            g2.fill(new Ellipse2D.Float(
                    BoardUtils.IndexTurnCoordinate(ghostX),
                    BoardUtils.IndexTurnCoordinate(ghostY),
                    GameConstants.ChessSize,
                    GameConstants.ChessSize
            ));
        } finally {
            g2.dispose();
        }

        lastGhostPos = new Point(ghostX, ghostY);
        lastGhostColor = currentColor;
        chessPanel.repaint();
    }

    public void clearGhost() {
        if (lastGhostPos == null) return;
        byte[][] panel = gameController.getGameState().getPanel();
        redrawFull(panel);
        lastGhostPos = null;
        lastGhostColor = 0;
        chessPanel.repaint();
    }

    // ==================== 其余不变的面板构建代码 ====================

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 15));
                g2d.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

                int size = 32;
                int y = (getHeight() - size) / 2;
                int leftX = 25;
                byte leftColor = (firstMovePlayer == 1 ? (byte)1 : (byte)2);
                int rightX = getWidth() - 25 - size;
                byte rightColor = (leftColor == 1) ? (byte)2 : (byte)1;

                drawIndicatorPiece(g2d, leftX, y, size, leftColor, currentActiveTurn == 1);
                drawIndicatorPiece(g2d, rightX, y, size, rightColor, currentActiveTurn == 2);
                g2d.dispose();
            }

            private void drawIndicatorPiece(Graphics2D g2d, int x, int y, int size, byte color, boolean isActive) {
                if (isActive) {
                    g2d.setColor(new Color(66, 165, 245, 40));
                    g2d.fill(new Ellipse2D.Float(x - 6, y - 6, size + 12, size + 12));
                    g2d.setColor(new Color(66, 165, 245, 180));
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.draw(new Ellipse2D.Float(x - 3, y - 3, size + 6, size + 6));
                } else {
                    g2d.setColor(new Color(0, 0, 0, 40));
                    g2d.fill(new Ellipse2D.Float(x + 1, y + 2, size, size));
                }

                if (color == 1) {
                    g2d.setColor(GameConstants.COLOR_PIECE_BLACK);
                    g2d.fill(new Ellipse2D.Float(x, y, size, size));
                } else {
                    g2d.setColor(GameConstants.COLOR_PIECE_WHITE);
                    g2d.fill(new Ellipse2D.Float(x, y, size, size));
                    g2d.setColor(new Color(180, 180, 180));
                    g2d.draw(new Ellipse2D.Float(x, y, size, size));
                }

                if (!isActive) {
                    g2d.setColor(new Color(30, 30, 36, 100));
                    g2d.fill(new Ellipse2D.Float(x, y, size, size));
                }
            }
        };

        topPanel.setPreferredSize(new Dimension(GameConstants.PanelWidth, GameConstants.PanelHeight));
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(GameConstants.COLOR_CARD_NAV);

        String leftName = (gameMode == GameConstants.GameMode.VS_AI) ? "玩家" : "玩家一";
        String rightName = (gameMode == GameConstants.GameMode.VS_AI) ? "电脑" : "玩家二";

        leftPlayerName = new JLabel(leftName, SwingConstants.LEFT);
        leftPlayerName.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        leftPlayerName.setForeground(Color.WHITE);
        leftPlayerName.setBorder(BorderFactory.createEmptyBorder(0, 70, 0, 0));
        topPanel.add(leftPlayerName, BorderLayout.WEST);

        scoreLabel = new JLabel("0 : 0", SwingConstants.CENTER);
        scoreLabel.setForeground(new Color(220, 220, 225));
        scoreLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        topPanel.add(scoreLabel, BorderLayout.CENTER);

        rightPlayerName = new JLabel(rightName, SwingConstants.RIGHT);
        rightPlayerName.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        rightPlayerName.setForeground(Color.WHITE);
        rightPlayerName.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 70));
        topPanel.add(rightPlayerName, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 14));
        bottomPanel.setPreferredSize(new Dimension(GameConstants.PanelWidth, GameConstants.PanelHeight));
        bottomPanel.setBackground(GameConstants.COLOR_CARD_NAV);

        JButton undoButton = new JButton("悔棋");
        JButton restartButton = new JButton("重新开始");
        JButton backButton = new JButton("返回主页");

        styleModernButton(undoButton, new Color(52, 152, 219), new Color(41, 128, 185));
        styleModernButton(restartButton, new Color(46, 139, 87), new Color(60, 179, 113));
        styleModernButton(backButton, new Color(231, 76, 60), new Color(192, 41, 43));

        undoButton.addActionListener(e -> gameController.Undo());
        restartButton.addActionListener(e -> gameController.RestartCurrentGame());
        backButton.addActionListener(e -> {
            ModernConfirmDialog dialog = new ModernConfirmDialog(this, "结束对局", "确定要结束当前对局返回主页吗？");
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                outAndExit();
            }
        });

        bottomPanel.add(undoButton);
        bottomPanel.add(restartButton);
        bottomPanel.add(backButton);
        return bottomPanel;
    }

    private void styleModernButton(AbstractButton button, Color normalColor, Color hoverColor) {
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(normalColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(hoverColor); button.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(normalColor); button.repaint(); }
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

    private boolean panelsEqual(byte[][] p1, byte[][] p2) {
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                if (p1[i][j] != p2[i][j]) return false;
            }
        }
        return true;
    }

    private byte[][] copyPanel(byte[][] src) {
        byte[][] copy = new byte[GameConstants.ChessBorderNum][GameConstants.ChessBorderNum];
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            System.arraycopy(src[i], 0, copy[i], 0, GameConstants.ChessBorderNum);
        }
        return copy;
    }

    private boolean isWinningPoint(int x, int y) {
        if (winningLine == null) return false;
        for (Point p : winningLine) {
            if (p.x == x && p.y == y) return true;
        }
        return false;
    }

    public void startWinAnimation(List<Point> winningLine, byte[][] panel, byte winnerColor) {
        this.winningLine = winningLine;
        this.winnerColor = winnerColor;
        flashState = true;
        if (animationTimer == null) {
            animationTimer = new Timer(400, e -> {
                flashState = !flashState;
                if (this.winningLine != null && panel != null) {
                    drawAllPieces(panel);
                }
                chessPanel.repaint();
            });
        }
        animationTimer.start();
    }

    public void stopWinAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        winningLine = null;
        flashState = false;
        winnerColor = 0;
        chessPanel.repaint();
    }

    public void updateTurnDisplay(byte playerFlag) {
        currentActiveTurn = playerFlag;
        if (playerFlag == 1) {
            leftPlayerName.setForeground(Color.WHITE);
            rightPlayerName.setForeground(new Color(130, 130, 140));
        } else {
            leftPlayerName.setForeground(new Color(130, 130, 140));
            rightPlayerName.setForeground(Color.WHITE);
        }
        repaint();
    }

    public void updateScore(int blackWins, int whiteWins) {
        scoreLabel.setText(blackWins + " : " + whiteWins);
    }

    public void outAndExit() {
        dispose();
        if (gameController != null) {
            gameController.End();
        }
    }

    public void addActionListener(ChessInputListener chessActionListener) {
        addBoardMouseListener(chessActionListener);
    }

    public void addBoardMouseListener(ChessInputListener chessActionListener) {
        removeBoardMouseListener(chessActionListener);
        chessPanel.addMouseListener(chessActionListener);
        chessPanel.addMouseMotionListener(chessActionListener);
    }

    public void showWinDialog(byte winnerColor) {
        String winnerName;
        Color dialogColor;
        byte winnerPlayer = gameController.getGameSystem().getGameState().getPlayerByColor(winnerColor);

        if (winnerPlayer == 1) {
            winnerName = "玩家1";
            dialogColor = new Color(52, 152, 219);
        } else {
            winnerName = (gameController.getGameSystem().getGameState().getGameMode() == GameConstants.GameMode.VS_AI) ? "智能AI" : "玩家2";
            dialogColor = new Color(241, 196, 15);
        }

        ModernWinDialog dialog = new ModernWinDialog(this, winnerName, dialogColor);
        dialog.setVisible(true);

        if (dialog.getChoice()) {
            gameController.RestartCurrentGame();
        } else {
            outAndExit();
        }
    }

    public void removeBoardMouseListener(ChessInputListener chessActionListener) {
        chessPanel.removeMouseListener(chessActionListener);
        chessPanel.removeMouseMotionListener(chessActionListener);
    }

    public void handleMouseMove(int boardX, int boardY) {
        if (lastHoverPoint != null && lastHoverPoint.x == boardX && lastHoverPoint.y == boardY) {
            return;
        }
        clearGhost();
        byte[][] panel = gameController.getGameState().getPanel();
        byte currentColor = gameController.getGameState().getCurrentPlayerColor();

        if (gameController.isValidMove(boardX, boardY)) {
            drawChessImg(panel, boardX, boardY, currentColor);
            lastHoverPoint = new Point(boardX, boardY);
        } else {
            handleMouseExit();
        }
    }

    public void handleMouseExit() {
        if (lastHoverPoint != null) {
            clearGhost();
            lastHoverPoint = null;
        }
    }

    public void clearHover() {
        lastHoverPoint = null;
    }
}