import java.awt.Point;

import ai.Minimax.MiniMaxEngine;
import utils.GameConstants;

public class EngineIsolationTest {
    public static void main(String[] args) {
        int size = GameConstants.ChessBorderNum;
        byte[][] mockBoard = new byte[size][size];

        // 阵营定义：1 = 黑棋（玩家）, 2 = 白棋（AI）
        byte P = 1; // Player
        byte A = 2; // AI

        System.out.println("==============================================");
        System.out.println("🧩 [五子棋 AI 真相盘面复刻测试]");
        System.out.println("正在将您截图中的真实棋盘局部坐标注入测试引擎...");
        System.out.println("==============================================");

        /* * 严格按照您第 3 张截图的中心密集交战区进行落子还原：
         * 以棋盘天元点 (7,7) 为核心辐射区
         */
        // ---- 第 4 行 ----
        mockBoard[4][7] = P;
        mockBoard[4][8] = A;
        mockBoard[4][10] = P;

        // ---- 第 5 行 ----
        mockBoard[5][5] = P;
        mockBoard[5][6] = A;
        mockBoard[5][7] = P;
        mockBoard[5][8] = P;
        mockBoard[5][9] = P;
        mockBoard[5][10] = A;

        // ---- 第 6 行 ----
        mockBoard[6][4] = P;
        mockBoard[6][5] = A;
        mockBoard[6][6] = A;
        mockBoard[6][7] = A;
        mockBoard[6][8] = P;

        // ---- 第 7 行 ----
        mockBoard[7][4] = A;
        mockBoard[7][5] = A;
        mockBoard[7][6] = P;
        mockBoard[7][7] = P;
        mockBoard[7][8] = P;
        mockBoard[7][9] = A;

        // ---- 第 8 行 ----
        mockBoard[8][3] = A;
        mockBoard[8][5] = A;
        mockBoard[8][6] = A;
        mockBoard[8][7] = P;
        mockBoard[8][8] = P;

        // ---- 第 9 行 ----
        mockBoard[9][5] = A;
        mockBoard[9][6] = P;
        mockBoard[9][7] = P;
        mockBoard[9][8] = A;

        // ---- 第 10 行 ----
        mockBoard[10][5] = P;
        mockBoard[10][7] = P;

        // ---- 第 11 行 ----
        mockBoard[11][7] = A;

        // 打印一个简易的控制台棋盘，方便肉眼核对
        printBoard(mockBoard);

        // 初始化 AI 引擎 (AI 持白棋，颜色代码为 2)
        MiniMaxEngine testEngine = new MiniMaxEngine();

        System.out.println("\n🤖 AI 正在基于此盘面计算最佳落子点...");
        long start = System.currentTimeMillis();
        Point bestMove = testEngine.findBestMove(mockBoard);
        long end = System.currentTimeMillis();

        System.out.println("\n================ 诊断报告 ================");
        System.out.println("⏱️ 算法思考耗时: " + (end - start) + " ms");
        System.out.println("🎯 AI 算出的最佳落子坐标: (" + bestMove.x + ", " + bestMove.y + ")");
        System.out.println("📝 当前坐标状态: " +
                (mockBoard[bestMove.x][bestMove.y] == 0 ? "空地（正常）" : "❌ 非空！覆盖了已有棋子！"));
        System.out.println("==========================================");

        System.out.println("\n💡 请核对：AI 给出的这个坐标 (" + bestMove.x + ", " + bestMove.y + ")，在你【游戏界面的视觉上】对应的哪个位置？");
        System.out.println("1. 如果在这个孤立测试里，AI 下的位置【正好堵住了你的四连/活三】，说明 AI 脑子没坏。");
        System.out.println("   👉 那么百分之百是外部调用时：`panel[x][y]` 在传给 `findBestMove` 之前，行和列倒过来了！");
        System.out.println("2. 如果在这个孤立测试里，AI 依然胡乱下在一个不相干的空地上，说明是算法估值失效。");
    }

    // 辅助打印棋盘的方法
    private static void printBoard(byte[][] board) {
        System.out.print("   ");
        for (int j = 0; j < board.length; j++) System.out.print(String.format("%2d", j));
        System.out.println();
        for (int i = 0; i < board.length; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 1) System.out.print(" ●"); // 黑
                else if (board[i][j] == 2) System.out.print(" ○"); // 白
                else System.out.print(" ·");
            }
            System.out.println();
        }
    }
}