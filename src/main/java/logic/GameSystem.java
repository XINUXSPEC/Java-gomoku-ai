package logic;

import Controller.GameController;
import State.GameState;
import ai.AI_Engine;
import ai.KataGomo.KataGomoEngine;
import ai.Minimax.MiniMaxEngine;
import utils.GameConstants;

import javax.swing.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 游戏系统核心类
 * <p>
 * 职责：
 * 1. 管理游戏状态（棋盘、回合、历史记录）
 * 2. 执行游戏逻辑（落子、悔棋、胜负判断）
 * 3. 协调AI计算
 * 4. 通过回调通知外部状态变化
 */
public class GameSystem {

    // ==================== 成员变量 ====================

    /** 游戏状态数据 */
    private final GameState gameState;

    /** 移动历史栈 - 用于悔棋和追踪上一手位置 */
    private final Stack<MoveHistory> moveStack;

    /** 当前使用的AI引擎（由 selectedEngine 决定） */
    private AI_Engine ai;

    /** 会话ID：每次 initGameState 自增，用于识别旧AI线程的回调是否属于当前会话 */
    private volatile long sessionId = 0;

    /** MiniMax 引擎实例（启动时创建，整个会话不销毁） */
    private final MiniMaxEngine miniMaxEngine;

    /** KataGomo 引擎实例（启动时创建，整个会话不销毁） */
    private final KataGomoEngine kataGomoEngine;

    /** 当前选定的引擎类型 */
    private GameConstants.AIEngine selectedEngine = GameConstants.AIEngine.MINIMAX;

    /** AI难度设置 */
    private GameConstants.AIDifficulty aiDifficulty = GameConstants.AIDifficulty.NORMAL;

    /** 黑棋获胜次数 */
    private int blackWins = 0;

    /** 白棋获胜次数 */
    private int whiteWins = 0;

    /** 控制器回调 - 用于通知状态变化 */
    private GameController controller;

    /** AI工作线程池 */
    private final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor aiWorker = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue
    );

    // ==================== 构造函数 ====================

    /**
     * 构造函数：一次性创建两个AI实例，整个会话不复用不销毁
     *
     * @param controller 游戏控制器，用于回调通知
     */
    public GameSystem(GameController controller) {
        this.gameState = new GameState();
        this.moveStack = new Stack<>();
        this.controller = controller;

        // 一次性创建两个AI（整个会话不复用）
        this.miniMaxEngine = new MiniMaxEngine();
        this.kataGomoEngine = new KataGomoEngine();

        // 默认 AI 为 null，等 initGameSystem 时选定
        this.ai = null;

        // 后台预热 KataGomo 进程（避免第一次进入游戏时等很久）
        aiWorker.submit(this::prewarmKataGomo);
    }

    // ==================== 游戏生命周期方法 ====================

    /**
     * 初始化游戏状态并启动AI
     *
     * @param mode            游戏模式（双人对战/人机对战）
     * @param firstMovePlayer 先手玩家（1或2）
     * @param aiDifficulty    AI难度
     * @param selectedEngine   选定的AI引擎
     */
    public void initGameSystem(GameConstants.GameMode mode,
                              byte firstMovePlayer,
                              GameConstants.AIDifficulty aiDifficulty,
                              GameConstants.AIEngine selectedEngine) {
        this.aiDifficulty = aiDifficulty == null ? GameConstants.AIDifficulty.NORMAL : aiDifficulty;
        gameState.initialize(mode, firstMovePlayer);

        // 重置历史记录
        moveStack.clear();

        // 自增会话ID，让旧AI线程的回调作废
        sessionId++;

        // 通知控制器游戏已开始（用于绘制棋盘）
        controller.handleGameStarted();
        initAIAsync(selectedEngine);
    }

    /**
     * 重新开始当前对局
     * - 切换先手玩家
     * - 调用 ai.Init() 清空AI内部状态（不销毁进程）
     */
    public void restartCurrentGame() {
        byte switchedFirstPlayer = (byte) (3 - gameState.getFirstMovePlayer());
        gameState.initialize(gameState.getGameMode(), switchedFirstPlayer);
        moveStack.clear();

        sessionId++;

        controller.handleGameReset();
        if (gameState.getGameMode() == GameConstants.GameMode.VS_AI && ai != null) {
            initAIAsync(this.selectedEngine);
        }
    }

    /**
     * 重置游戏状态（仅清除比分和状态，不销毁AI）
     */
    public void resetGameState() {
        gameState.setRuing(false);
        System.out.println("[游戏] 重置状态...");

        // 重置比分
        blackWins = 0;
        whiteWins = 0;

        System.out.println("[游戏] 状态已重置");
    }

    /**
     * 彻底销毁AI引擎，释放所有资源（程序退出时调用）
     * KataGomo的子进程必须显式销毁，否则JVM退出后变成孤儿进程
     */
    public void shutdownAI() {
        System.out.println("[游戏] 正在彻底销毁AI引擎...");

        aiWorker.submit(() -> {
            if (miniMaxEngine != null) {
                miniMaxEngine.dispose();
            }
            if (kataGomoEngine != null) {
                kataGomoEngine.dispose();
            }
            ai = null;
            System.out.println("[游戏] AI引擎已彻底销毁");
        });
    }

    public GameConstants.GameMode GetMode(){
        return this.gameState.getGameMode();
    }
    // ==================== 游戏操作方法 ====================

    /**
     * 玩家落子
     *
     * @param x 棋盘行坐标（0-14）
     * @param y 棋盘列坐标（0-14）
     * @return 是否落子成功
     */
    public boolean playMove(int x, int y) {
        // 检查位置是否为空
        if (!gameState.isEmpty(x, y)) {
            return false;
        }

        byte currentColor = gameState.getCurrentPlayerColor();

        // 记录历史
        moveStack.push(new MoveHistory(x, y, currentColor));

        // 更新棋盘
        gameState.setPiece(x, y, currentColor);

        // 检查胜利
        List<Point> winLine = checkWin(x, y);
        if (winLine != null) {
            updateWinCount(currentColor);
            controller.handleWin(currentColor, winLine);
            return true;
        }

        // 检查平局
        if (isBoardFull()) {
            updateDrawCount();
            controller.handleDraw();
            return true;
        }

        // 切换回合
        gameState.switchTurn();
        controller.handleTurnChanged();

        // AI回合（如果是人机对战模式且当前是AI回合）
        if (gameState.getGameMode() == GameConstants.GameMode.VS_AI &&
                gameState.getCurrentTurn() == 2) {
            playAIMove();
        }
        return true;
    }

    /**
     * 悔棋
     */
    public void undoMove() {
        if (!canUndo()) {
            return;
        }

        if (gameState.getGameMode() == GameConstants.GameMode.VS_AI) {
            undoAIModeMove();
        } else {
            undoPlayerModeMove();
        }

        controller.handleGameReset();
    }

    // ==================== AI相关方法 ====================

    /**
     * 预热KataGomo进程（后台执行，避免第一次进入游戏时等待）
     */
    private void prewarmKataGomo() {
        try {
            System.out.println("[GameSystem] 预热启动 KataGomo 进程...");
            kataGomoEngine.LoadMode();
            System.out.println("[GameSystem] KataGomo 预热完成");
        } catch (Exception e) {
            System.out.println("[GameSystem] KataGomo 预热出错: " + e);
        }
    }

    /**
     * 在后台线程中初始化选定的AI引擎
     * AI实例在GameSystem构造时已创建，本方法只是选择+Init
     *
     * @param selectedEngine 选定的AI引擎
     */
    private void initAIAsync(GameConstants.AIEngine selectedEngine) {
        if (gameState.getGameMode() != GameConstants.GameMode.VS_AI) {
            return;
        }

        // 1. 选择AI实例（只设置ai字段，不创建新对象）
        this.selectedEngine = selectedEngine;
        this.ai = (selectedEngine == GameConstants.AIEngine.MINIMAX) ? miniMaxEngine : kataGomoEngine;

        byte aiColor = gameState.getPlayerColor(2);
        final long mySessionId = sessionId;

        aiWorker.submit(() -> {
            try {
                System.out.println("[游戏] 正在初始化AI: " + selectedEngine);
                ai.Init(aiColor);

                // 检查会话是否仍然有效
                if (mySessionId != sessionId) {
                    System.out.println("[游戏] 会话已变更，放弃AI初始化");
                    return;
                }

                System.out.println("[游戏] AI初始化完成");

                // AI先手开局：如果AI先手且还没有落子，自动下开局第一步
                if (gameState.getGameMode() == GameConstants.GameMode.VS_AI
                        && gameState.getCurrentTurn() == 2
                        && !canUndo()) {
                    executeAIOpeningMove(aiColor, mySessionId);
                }
            } catch (Exception e) {
                System.err.println("[游戏] AI初始化异常: " + e.getMessage());
            }
        });
    }

    /**
     * AI先手开局落子（从initAIAsync中提取）
     */
    private void executeAIOpeningMove(byte aiColor, long mySessionId) {
        System.out.println("[线程调度] 后台 AI 计算线程已拉起，开始压榨算力...");

        final byte[][] boardSnapshot = copyBoard(gameState.getPanel());
        final Point lastMove = getLastMoveFromStack();
        final Point p = ai.findBestMove(boardSnapshot, lastMove);

        // 检查会话是否变更
        if (mySessionId != sessionId) {
            System.out.println("[线程调度] 会话已变更，放弃旧AI落子");
            return;
        }
        if (!gameState.isRuing()) {
            System.out.println("[线程调度] 游戏已结束");
            return;
        }
        if (p == null) {
            System.err.println("[警告] AI 未能计算出有效落子位置");
            return;
        }

        // 执行落子
        moveStack.push(new MoveHistory(p.x, p.y, aiColor));
        gameState.setPiece(p.x, p.y, aiColor);
        List<Point> winLine = checkWin(p.x, p.y);

        SwingUtilities.invokeLater(() -> {
            try {
                if (winLine != null) {
                    updateWinCount(aiColor);
                    controller.handleWin(aiColor, winLine);
                    return;
                }

                if (isBoardFull()) {
                    updateDrawCount();
                    controller.handleDraw();
                    return;
                }

                gameState.switchTurn();
                controller.handleTurnChanged();
            } finally {
                System.out.println("[线程调度] AI 落子完毕");
            }
        });
    }

    /**
     * AI自动落子（玩家回合结束后触发）
     */
    private void playAIMove() {
        final byte[][] boardSnapshot = copyBoard(gameState.getPanel());
        final Point lastMove = getLastMoveFromStack();
        final byte aiColor = gameState.getCurrentPlayerColor();
        final long mySessionId = sessionId;

        aiWorker.submit(() -> {
            try {
                System.out.println("[线程调度] 后台 AI 计算线程已拉起，开始压榨算力...");

                final Point p = ai.findBestMove(boardSnapshot, lastMove);

                // 检查会话是否变更
                if (mySessionId != sessionId) {
                    System.out.println("[线程调度] 会话已变更，放弃旧AI落子");
                    return;
                }
                if (!gameState.isRuing()) {
                    System.out.println("[线程调度] 游戏已结束");
                    return;
                }
                if (p == null) {
                    System.err.println("[警告] AI 未能计算出有效落子位置");
                    return;
                }

                // 执行落子
                moveStack.push(new MoveHistory(p.x, p.y, aiColor));
                gameState.setPiece(p.x, p.y, aiColor);
                List<Point> winLine = checkWin(p.x, p.y);

                SwingUtilities.invokeLater(() -> {
                    try {
                        if (winLine != null) {
                            updateWinCount(aiColor);
                            controller.handleWin(aiColor, winLine);
                            return;
                        }

                        if (isBoardFull()) {
                            updateDrawCount();
                            controller.handleDraw();
                            return;
                        }

                        gameState.switchTurn();
                        controller.handleTurnChanged();
                    } finally {
                        System.out.println("[线程调度] AI 落子完毕");
                    }
                });
            } catch (Exception e) {
                System.err.println("[错误] 后台 AI 线程发生异常！");
                e.printStackTrace();
            }
        });
    }

    // ==================== 胜负判断方法 ====================

    /**
     * 检查指定位置是否获胜
     *
     * @param x 检查的行坐标
     * @param y 检查的列坐标
     * @return 获胜的五子连线，如果没有获胜返回null
     */
    private List<Point> checkWin(int x, int y) {
        byte[][] panel = gameState.getPanel();
        byte currentPlayerFlag = panel[x][y];

        // 四个方向：水平、垂直、主对角线、副对角线
        int[][] directions = {
                {1, 0}, {0, 1}, {1, 1}, {1, -1}
        };

        for (int[] d : directions) {
            List<Point> winLine = checkDirection(panel, x, y, d[0], d[1], currentPlayerFlag);
            if (winLine != null) {
                return winLine;
            }
        }
        return null;
    }

    /**
     * 检查指定方向是否形成五子连珠
     *
     * @param panel      棋盘数据
     * @param x          起始X坐标
     * @param y          起始Y坐标
     * @param dx         X方向增量
     * @param dy         Y方向增量
     * @param playerFlag 玩家标志
     * @return 五子连线，如果没有返回null
     */
    private List<Point> checkDirection(byte[][] panel, int x, int y, int dx, int dy, byte playerFlag) {
        List<Point> currentLine = new ArrayList<>();
        int count = 1;
        currentLine.add(new Point(x, y));

        // 正向检查
        for (int i = 1; i < 5; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;
            if (isLegal(nx, ny) && panel[nx][ny] == playerFlag) {
                count++;
                currentLine.add(new Point(nx, ny));
            } else {
                break;
            }
        }

        // 反向检查
        for (int i = 1; i < 5; i++) {
            int nx = x - i * dx;
            int ny = y - i * dy;
            if (isLegal(nx, ny) && panel[nx][ny] == playerFlag) {
                count++;
                currentLine.add(new Point(nx, ny));
            } else {
                break;
            }
        }

        // 五子连珠判定
        if (count >= 5) {
            return currentLine;
        }
        return null;
    }

    /**
     * 检查棋盘是否已满
     *
     * @return 是否已满
     */
    private boolean isBoardFull() {
        byte[][] panel = gameState.getPanel();
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                if (panel[i][j] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 更新获胜次数统计
     *
     * @param winnerColor 获胜方颜色
     */
    private void updateWinCount(byte winnerColor) {
        byte winnerPlayer = gameState.getPlayerByColor(winnerColor);
        if (winnerPlayer == 1) {
            blackWins++;
        } else if (winnerPlayer == 2) {
            whiteWins++;
        }
    }

    /**
     * 更新平局次数统计（各加1）
     */
    private void updateDrawCount() {
        blackWins++;
        whiteWins++;
    }

    // ==================== 悔棋相关方法 ====================

    /**
     * 人机对战模式悔棋（撤销两步：玩家+AI）
     */
    private void undoAIModeMove() {
        int stepCount = moveStack.size();

        // 特殊情况：只剩一步（玩家刚下完第一步）
        if (stepCount == 1) {
            MoveHistory move = moveStack.pop();
            gameState.setPiece(move.x, move.y, (byte) 0);
            gameState.setCurrentTurn((byte) 2);
        } else {
            // 正常情况：撤销两步（玩家+AI各一步）
            for (int i = 0; i < 2; i++) {
                MoveHistory move = moveStack.pop();
                gameState.setPiece(move.x, move.y, (byte) 0);
                gameState.switchTurn();
            }
        }
    }

    /**
     * 双人对战模式悔棋（撤销一步）
     */
    private void undoPlayerModeMove() {
        MoveHistory move = moveStack.pop();
        gameState.setPiece(move.x, move.y, (byte) 0);
        gameState.switchTurn();
    }

    // ==================== 状态查询方法 ====================

    /**
     * 获取游戏状态
     *
     * @return 游戏状态对象
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * 获取黑棋获胜次数
     *
     * @return 黑棋获胜次数
     */
    public int getBlackWins() {
        return blackWins;
    }

    /**
     * 获取白棋获胜次数
     *
     * @return 白棋获胜次数
     */
    public int getWhiteWins() {
        return whiteWins;
    }

    /**
     * 获取最后一步落子位置
     *
     * @return 最后一步的坐标，如果没有返回null
     */
    public Point getLastMove() {
        if (moveStack.isEmpty()) {
            return null;
        }
        MoveHistory last = moveStack.peek();
        return new Point(last.x, last.y);
    }

    /**
     * 是否可以悔棋
     *
     * @return 是否有可悔的棋
     */
    public boolean canUndo() {
        return !moveStack.isEmpty();
    }

    /**
     * 当前是否是AI回合
     *
     * @return 是否是AI回合
     */
    public boolean isAiTurn() {
        return gameState.getCurrentTurn() == 2;
    }

    /**
     * AI是否忙碌（正在初始化或计算中）
     *
     * @return AI是否忙碌
     */
    public boolean isAiBusy() {
        // 排队等待的任务数 + 正在执行的任务数
        int totalTasks = taskQueue.size() + aiWorker.getActiveCount();
        return totalTasks > 0;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从历史记录中获取上一手落子位置（用于AI决策）
     */
    private Point getLastMoveFromStack() {
        if (moveStack.isEmpty()) {
            return null;
        }
        MoveHistory last = moveStack.peek();
        return new Point(last.x, last.y);
    }

    /**
     * 检查坐标是否在棋盘范围内
     *
     * @param x X坐标
     * @param y Y坐标
     * @return 是否合法
     */
    private boolean isLegal(int x, int y) {
        return x >= 0 && x < GameConstants.ChessBorderNum && y >= 0 && y < GameConstants.ChessBorderNum;
    }

    /**
     * 复制棋盘（深拷贝，防止外部修改）
     *
     * @param src 源棋盘
     * @return 复制后的棋盘
     */
    private byte[][] copyBoard(byte[][] src) {
        byte[][] copy = new byte[src.length][src[0].length];
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, copy[i], 0, src[i].length);
        }
        return copy;
    }

    // ==================== 内部类 ====================

    /**
     * 移动历史记录
     */
    private record MoveHistory(int x, int y, byte color) {
    }
}
