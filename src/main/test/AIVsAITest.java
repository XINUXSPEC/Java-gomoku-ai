import ai.KataGomo.KataGomoEngine;
import ai.Minimax.MiniMaxEngine;
import utils.GameConstants;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * AI对战测试类
 * 支持先后手交替：
 * 奇数局：MiniMax（黑棋/先手） vs KataGomo（白棋/后手）
 * 偶数局：KataGomo（黑棋/先手） vs MiniMax（白棋/后手）
 */
public class AIVsAITest {

    private static final int TOTAL_GAMES = 100;
    private static final int MINIMAX_DEPTH = 8;

    // 棋盘大小
    private static final int BOARD_SIZE = GameConstants.ChessBorderNum;

    // 棋子代号定义 (通常五子棋/围棋中 1为黑，2为白)
    private static final byte BLACK = 1;
    private static final byte WHITE = 2;

    // 统计结果
    private int minimaxWins = 0;   // MiniMax 胜利次数
    private int kataGomoWins = 0;  // KataGomo 胜利次数
    private int draws = 0;         // 平局次数

    // AI 引擎
    private MiniMaxEngine minimaxEngine;
    private KataGomoEngine kataGomoEngine;

    public static void main(String[] args) {
        AIVsAITest test = new AIVsAITest();
        test.runTest();
    }

    public void runTest() {
        System.out.println("========================================");
        System.out.println("AI 对战测试开始（先后手交替模式）");
        System.out.println("MiniMax 搜索深度: " + MINIMAX_DEPTH);
        System.out.println("总对局数: " + TOTAL_GAMES);
        System.out.println("========================================\n");

        // 初始化 AI 引擎
        if (!initEngines()) {
            System.err.println("AI 引擎初始化失败，测试终止");
            return;
        }

        // 进行对局
        long startTime = System.currentTimeMillis();
        for (int game = 1; game <= TOTAL_GAMES; game++) {
            // 奇数局 MiniMax 先手(黑)，偶数局 KataGomo 先手(黑)
            boolean isMinimaxBlack = (game % 2 != 0);

            System.out.print("第 " + game + " 局 [" + (isMinimaxBlack ? "MiniMax先手" : "KataGomo先手") + "]: ");

            int winningColor = playOneGame(isMinimaxBlack);

            if (winningColor == BLACK) {
                if (isMinimaxBlack) {
                    minimaxWins++;
                    System.out.println("MiniMax 胜利 (先手黑棋)");
                } else {
                    kataGomoWins++;
                    System.out.println("KataGomo 胜利 (先手黑棋)");
                }
            } else if (winningColor == WHITE) {
                if (isMinimaxBlack) {
                    kataGomoWins++;
                    System.out.println("KataGomo 胜利 (后手白棋)");
                } else {
                    minimaxWins++;
                    System.out.println("MiniMax 胜利 (后手白棋)");
                }
            } else {
                draws++;
                System.out.println("平局");
            }

            // 每10局输出一次统计
            if (game % 10 == 0) {
                printStatistics(game);
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;

        // 输出最终结果
        System.out.println("\n========================================");
        System.out.println("测试完成！总耗时: " + (elapsed / 1000.0) + " 秒");
        printStatistics(TOTAL_GAMES);

        // 销毁引擎
        cleanup();
    }

    /**
     * 初始化 AI 引擎
     */
    private boolean initEngines() {
        System.out.println("正在初始化 AI 引擎...");

        // MiniMax 引擎（8层深度）
        System.out.println("  初始化 MiniMax 引擎...");
        minimaxEngine = new MiniMaxEngine(MINIMAX_DEPTH, 5000);
        // 这里只是初次激活，实际每局开局会重新 Init 设定角色
        minimaxEngine.Init(BLACK);
        System.out.println("  MiniMax 引擎初始化完成");

        // KataGomo 引擎
        System.out.println("  初始化 KataGomo 引擎...");
        kataGomoEngine = new KataGomoEngine();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            try {
                kataGomoEngine.LoadMode();
                kataGomoEngine.Init(WHITE);
                return true;
            } catch (Exception e) {
                System.err.println("KataGomo 初始化异常: " + e.getMessage());
                return false;
            }
        });

        try {
            Boolean success = future.get(60, TimeUnit.SECONDS);
            if (success != null && success) {
                System.out.println("  KataGomo 引擎初始化完成");
            } else {
                System.err.println("  KataGomo 引擎初始化失败");
                return false;
            }
        } catch (TimeoutException e) {
            System.err.println("  KataGomo 引擎初始化超时（60秒）");
            future.cancel(true);
            return false;
        } catch (Exception e) {
            System.err.println("  KataGomo 引擎初始化异常: " + e.getMessage());
            return false;
        } finally {
            executor.shutdown();
        }

        System.out.println("AI 引擎初始化完成\n");
        return true;
    }

    /**
     * 进行一局对弈
     * @param isMinimaxBlack 当前局 MiniMax 是否执黑（先手）
     * @return 1=黑棋胜, 2=白棋胜, 0=平局
     */
    private int playOneGame(boolean isMinimaxBlack) {
        // 1. 初始化空棋盘
        byte[][] board = new byte[BOARD_SIZE][BOARD_SIZE];

        // 2. 根据本局阵营，动态为引擎初始化角色代号
        byte minimaxColor = isMinimaxBlack ? BLACK : WHITE;
        byte kataGomoColor = isMinimaxBlack ? WHITE : BLACK;

        minimaxEngine.Init(minimaxColor);
        kataGomoEngine.Init(kataGomoColor);

        // 3. 游戏从黑棋（先手=1）开始
        int currentTurn = BLACK;
        int moveCount = 0;
        final int MAX_MOVES = BOARD_SIZE * BOARD_SIZE;

        while (moveCount < MAX_MOVES) {
            Point move;

            // 4. 判断当前轮到哪个引擎落子
            if (currentTurn == minimaxColor) {
                move = minimaxEngine.findBestMove(board);
            } else {
                move = kataGomoEngine.findBestMove(board);
            }

            if (move == null) {
                System.out.print("[引擎返回null异常] ");
                break;
            }

            // 5. 落子并记录
            board[move.x][move.y] = (byte) currentTurn;
            moveCount++;

            // 6. 检查胜负
            List<Point> winLine = checkWin(board, move.x, move.y, (byte) currentTurn);
            if (winLine != null) {
                return currentTurn; // 返回获胜的棋子颜色 (1或2)
            }

            // 7. 切换回合 (3 - 1 = 2, 3 - 2 = 1)
            currentTurn = 3 - currentTurn;
        }

        // 平局
        return 0;
    }

    /**
     * 检查是否获胜
     */
    private List<Point> checkWin(byte[][] board, int x, int y, byte player) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] d : directions) {
            List<Point> winLine = checkDirection(board, x, y, d[0], d[1], player);
            if (winLine != null) {
                return winLine;
            }
        }
        return null;
    }

    /**
     * 检查指定方向是否五子连珠
     */
    private List<Point> checkDirection(byte[][] board, int x, int y, int dx, int dy, byte player) {
        List<Point> line = new ArrayList<>();
        int count = 1;
        line.add(new Point(x, y));

        // 正向
        for (int i = 1; i < 5; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;
            if (isLegal(nx, ny) && board[nx][ny] == player) {
                count++;
                line.add(new Point(nx, ny));
            } else {
                break;
            }
        }

        // 反向
        for (int i = 1; i < 5; i++) {
            int nx = x - i * dx;
            int ny = y - i * dy;
            if (isLegal(nx, ny) && board[nx][ny] == player) {
                count++;
                line.add(new Point(nx, ny));
            } else {
                break;
            }
        }

        if (count >= 5) {
            return line;
        }
        return null;
    }

    /**
     * 检查坐标是否合法
     */
    private boolean isLegal(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    /**
     * 输出统计信息
     */
    private void printStatistics(int gamesPlayed) {
        System.out.println("----------------------------------------");
        System.out.println("当前统计（已完成 " + gamesPlayed + " 局）:");
        System.out.println("  MiniMax 胜利: " + minimaxWins + " 局 ("
                + String.format("%.1f", 100.0 * minimaxWins / gamesPlayed) + "%)");
        System.out.println("  KataGomo 胜利: " + kataGomoWins + " 局 ("
                + String.format("%.1f", 100.0 * kataGomoWins / gamesPlayed) + "%)");
        System.out.println("  平局: " + draws + " 局 ("
                + String.format("%.1f", 100.0 * draws / gamesPlayed) + "%)");
        System.out.println("----------------------------------------");
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if (minimaxEngine != null) {
            minimaxEngine.dispose();
        }
        if (kataGomoEngine != null) {
            kataGomoEngine.dispose();
        }
        System.out.println("AI 引擎已销毁");
    }
}