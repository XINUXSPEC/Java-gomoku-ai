package Controller;

import   State.GameState;
import   logic.ChessInputListener;
import   logic.GameSystem;
import   ui.GameGui;
import ui.Dialog.ModernConfirmDialog;

import   ui.StartGameGUI;
import ui.ToastNotification;
import   utils.GameConstants;
import   utils.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 游戏控制器 - 协调器
 * 
 * 职责：
 * 1. 协调 GameSystem、GameGui、ChessInputListener 之间的交互
 * 2. 处理游戏事件（落子、悔棋、重新开始等）
 * 3. 管理 UI 更新
 */
public class GameController {
    private final GameSystem gameSystem;
    private final StartGameGUI startPage;
    private GameGui gameGui;
    private ChessInputListener chessController;

    public GameController(StartGameGUI gui) {
        this.startPage = gui;
        this.gameSystem = new GameSystem(this);
    }

    public void Start(GameConstants.GameMode mode,
                      byte firstMovePlayer,
                      GameConstants.AIDifficulty aiDifficulty,
                      GameConstants.AIEngine selectedEngine) {
        // 1. 立即关闭旧窗口（如果存在）
        if (gameGui != null) {
            gameGui.dispose();
            gameGui = null;
        }

        // 2. 立即显示新 GameGui 窗口
        gameGui = new GameGui(this, mode, firstMovePlayer);
        chessController = new ChessInputListener(this);
        gameGui.addActionListener(chessController);

        // 3. 初始化游戏状态
        gameSystem.initGameSystem(mode, firstMovePlayer, aiDifficulty,selectedEngine);

        // 4. 立即刷新棋盘
        gameGui.refreshBoard();
    }
    public void End() {
        // 1. 关闭游戏窗口
        if (gameGui != null) {
            gameGui.dispose();
            gameGui = null;
        }
        // 2. 显示主页
        startPage.setVisible(true);
        // 3. 重置游戏状态
        gameSystem.resetGameState();
    }
    public void shutdownAI() {
        gameSystem.shutdownAI();
    }
    public void Undo() {
        if (!gameSystem.canUndo()) {
            JOptionPane.showMessageDialog(gameGui, "没有可悔的棋！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        gameSystem.undoMove();
    }
    public void RestartCurrentGame() {
        ModernConfirmDialog dialog = new ModernConfirmDialog(
                gameGui,
                "重新开始",
                "确定要重新开始当前对局吗？"
        );
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            gameSystem.restartCurrentGame();
        } else {
            // 点击"否"时返回主页
            End();
        }
    }

    public void handleGameStarted() {
        gameGui.updateTurnDisplay(gameSystem.getGameState().getCurrentTurn());
        gameGui.updateScore(gameSystem.getBlackWins(), gameSystem.getWhiteWins());
        gameGui.drawAllPieces(gameSystem.getGameState().getPanel());
    }
    public void handleTurnChanged() {
        gameGui.drawAllPieces(gameSystem.getGameState().getPanel());
        gameGui.updateTurnDisplay(gameSystem.getGameState().getCurrentTurn());
        SoundManager.playMoveSound();
    }
    public void handleWin(byte winnerColor, List<Point> winningLine) {
        SoundManager.playMoveSound();
        gameGui.startWinAnimation(winningLine, gameSystem.getGameState().getPanel(), winnerColor);
        gameGui.removeBoardMouseListener(chessController);
        gameGui.updateScore(gameSystem.getBlackWins(), gameSystem.getWhiteWins());
        gameGui.showWinDialog(winnerColor);
    }
    public void handleDraw() {
        int choice = JOptionPane.showConfirmDialog(
                gameGui,
                "棋盘已满，本局平局。是否再来一局？",
                "平局",
                JOptionPane.YES_NO_OPTION
        );
        if (choice == JOptionPane.YES_OPTION) {
            RestartCurrentGame();
        } else {
            gameGui.outAndExit();
        }
    }
    public void handleGameReset() {
        // 切换先手后更新顶栏头像
        byte newFirstPlayer = gameSystem.getGameState().getCurrentTurn();
        gameGui.setFirstMovePlayer(newFirstPlayer);
        gameGui.clearHover();
        gameGui.stopWinAnimation();
        gameGui.addBoardMouseListener(chessController);
        gameGui.drawAllPieces(gameSystem.getGameState().getPanel());
        gameGui.updateTurnDisplay(gameSystem.getGameState().getCurrentTurn());
    }

    /**
     * 处理鼠标移动 - 转发给 GameGui 显示残影
     */
    public void handleMouseMove(int boardX, int boardY) {
        gameGui.handleMouseMove(boardX, boardY);
    }
    /**
     * 处理鼠标离开 - 转发给 GameGui 清除残影
     */
    public void handleMouseExit() {
        gameGui.handleMouseExit();
    }
    public void handleBoardClick(int boardX, int boardY) {
        if (gameSystem.GetMode() == GameConstants.GameMode.VS_AI && gameSystem.isAiTurn() && gameSystem.isAiBusy()) {
            gameGui.showToast("AI 正在思考中，请勿重复点击");
            return;
        }
        if (!isValidMove(boardX, boardY)) {
            return;
        }
        gameSystem.playMove(boardX, boardY);
    }

    public boolean isValidMove(int x, int y) {
        return gameSystem.getGameState().isEmpty(x, y);
    }
    public GameState getGameState() {
        return gameSystem.getGameState();
    }
    public GameSystem getGameSystem() {
        return gameSystem;
    }
}
