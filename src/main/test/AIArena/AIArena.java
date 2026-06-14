package AIArena;

import ai.AI_Engine;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

public class AIArena {

    private final AI_Engine engineA;
    private final AI_Engine engineB;
    private final int boardSize = 15;

    public AIArena(AI_Engine engineA, AI_Engine engineB) {
        this.engineA = engineA;
        this.engineB = engineB;
    }

    public void startTournament(int totalGames, boolean autoRotateColor) {
        List<Double> winRateHistoryA = new ArrayList<>();
        List<Double> winRateHistoryB = new ArrayList<>();

        int winA = 0;
        int winB = 0;
        int draw = 0;

        // 🎯 核心增强：动态提取类名以及内部可能存在的“层数/深度”参数
        String labelA = getEngineLabel(engineA);
        String labelB = getEngineLabel(engineB);

        String nameA = engineA.getClass().getSimpleName();
        String nameB = engineB.getClass().getSimpleName();

        System.out.println("=========================================================================");
        System.out.println("                     AI 对战擂台 (AI Arena) 终极版                       ");
        System.out.println("=========================================================================");
        System.out.printf("【参赛选手】 选手一：%s  vs  选手二：%s\n", labelA, labelB);
        System.out.printf("【对局设定】 总局数：%d 局  |  先后手轮转：%s\n", totalGames, autoRotateColor ? "开启" : "关闭");
        System.out.println("=========================================================================");

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalGames; i++) {
            System.out.printf("\n✨✨✨=============【第 %d / %d 局 擂台开始】=============✨✨✨\n", i, totalGames);

            byte roleA = autoRotateColor ? (byte) (i % 2 == 1 ? 1 : 2) : 1;
            byte roleB = (byte) (roleA == 1 ? 2 : 1);

            System.out.printf("[执标公告] %s 执 -> %s | %s 执 -> %s\n",
                    nameA, roleA == 1 ? "● 黑棋 (先手)" : "○ 白棋 (后手)",
                    nameB, roleB == 1 ? "● 黑棋 (先手)" : "○ 白棋 (后手)");
            System.out.println("-------------------------------------------------------------------------");

            engineA.Init(roleA);
            engineB.Init(roleB);

            int result = playSingleGame(roleA, roleB, nameA, nameB);

            if (result == 1) {
                winA++;
                System.out.printf("\n🎉 [局终战报] 本局结束：【%s】击败了【%s】赢下本局！\n", nameA, nameB);
            } else if (result == 2) {
                winB++;
                System.out.printf("\n🎉 [局终战报] 本局结束：【%s】击败了【%s】赢下本局！\n", nameB, nameA);
            } else {
                draw++;
                System.out.println("\n🤝 [局终战报] 本局结束：棋盘下满或异常，双方握手言和。");
            }

            winRateHistoryA.add((double) winA / i);
            winRateHistoryB.add((double) winB / i);
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\n=========================================================================");
        System.out.println("                      🏆 TOURNAMENT 比赛最终总决算 🏆                    ");
        System.out.println("=========================================================================");
        System.out.printf(" ⏳ 测试总耗时 : %.2f 秒\n", (endTime - startTime) / 1000.0);
        System.out.printf(" ⚔️ 【%s】 总胜局 : %d 局 (胜率: %.2f%%)\n", labelA, winA, (double) winA / totalGames * 100);
        System.out.printf(" ⚔️ 【%s】 总胜局 : %d 局 (胜率: %.2f%%)\n", labelB, winB, (double) winB / totalGames * 100);
        System.out.printf(" 🤝 平局/中止数 : %d 局\n", draw);
        System.out.println("=========================================================================");

        // 🎯 核心增强：将带有层数参数的标签输入给文件生成器
        ArenaDataPacker.generateReport(labelA, labelB, winRateHistoryA, winRateHistoryB);
    }

    /**
     * 🎯 极客私货：利用反射探针技术，自动嗅探传统博弈引擎内部的 maxDepth（最大层数）变量
     */
    private String getEngineLabel(AI_Engine engine) {
        String baseName = engine.getClass().getSimpleName();
        try {
            // 尝试去抓取实现类中的 maxDepth 属性字段
            Field depthField = engine.getClass().getDeclaredField("maxDepth");
            depthField.setAccessible(true); // 强行解除 private 访问权限限制
            int depth = depthField.getInt(engine);
            return baseName + " (Depth: " + depth + ")"; // 抓到了，返回带层数的标号
        } catch (NoSuchFieldException e) {
            // 如果类里面根本没有 maxDepth 字段（比如大模型 KataGomoEngine），就会触发此异常
            try {
                // 兼容有些代码可能会把变量名写成简写 depth 的情况
                Field depthField = engine.getClass().getDeclaredField("depth");
                depthField.setAccessible(true);
                int depth = depthField.getInt(engine);
                return baseName + " (Depth: " + depth + ")";
            } catch (Exception ex) {
                return baseName; // 实在没有层数属性，就安全返回原生类名
            }
        } catch (Exception e) {
            return baseName; // 其他未知反射异常安全兜底
        }
    }

    private int playSingleGame(byte roleA, byte roleB, String nameA, String nameB) {
        byte[][] board = new byte[boardSize][boardSize];
        Point lastMove = null;
        byte currentTurn = 1;
        int moveCount = 0;

        while (moveCount < boardSize * boardSize) {
            boolean isTurnA = (roleA == currentTurn);
            AI_Engine currentEngine = isTurnA ? engineA : engineB;
            String currentEngineName = isTurnA ? nameA : nameB;
            String stoneSymbol = (currentTurn == 1) ? "● 黑" : "○ 白";

            long pStart = System.currentTimeMillis();
            Point move;
            try {
                move = currentEngine.findBestMove(board, lastMove);
            } catch (Exception e) {
                System.err.printf("\n💥 核心警报：【%s】在思考时发生底层异常/闪退！直接判负。\n", currentEngineName);
                e.printStackTrace();
                return isTurnA ? 2 : 1;
            }
            long pTime = System.currentTimeMillis() - pStart;

            if (move == null) {
                System.out.printf("\n⚠️ 判负公告：【%s】返回了空坐标(null)放弃抵抗！\n", currentEngineName);
                return isTurnA ? 2 : 1;
            }
            if (move.x < 0 || move.x >= boardSize || move.y < 0 || move.y >= boardSize || board[move.x][move.y] != 0) {
                System.out.printf("\n⚠️ 判负公告：【%s】触发非法落子或越界坐标 %s ！\n", currentEngineName, move);
                return isTurnA ? 2 : 1;
            }

            board[move.x][move.y] = currentTurn;
            lastMove = move;
            moveCount++;

            String gtpString = toGtpString(move.x, move.y);

            System.out.printf("[步数:%03d] ♟️【%s】(%s) 落子 -> [数组坐标: x(行)=%2d, y(列)=%2d] | [GTP坐标: %3s] | 耗时: %4d ms\n",
                    moveCount, currentEngineName, stoneSymbol, move.x, move.y, gtpString, pTime);

            printBoard(board, move);

            if (checkWin(board, move.x, move.y, currentTurn)) {
                System.out.printf("\n👑 绝杀！【%s】执%s在 [%s] 连成五子，达成胜利！\n", currentEngineName, stoneSymbol, gtpString);
                return isTurnA ? 1 : 2;
            }

            currentTurn = (byte) (currentTurn == 1 ? 2 : 1);
        }
        return 0;
    }

    private void printBoard(byte[][] board, Point lastMove) {
        System.out.print("   ");
        char[] colLetters = "A B C D E F G H J K L M N O P".toCharArray();
        System.out.println(new String(colLetters));

        for (int r = 0; r < boardSize; r++) {
            System.out.printf("%2d ", r);
            for (int c = 0; c < boardSize; c++) {
                boolean isLast = (lastMove != null && lastMove.x == r && lastMove.y == c);

                String symbol;
                if (board[r][c] == 1) {
                    symbol = "●";
                } else if (board[r][c] == 2) {
                    symbol = "○";
                } else {
                    symbol = "·";
                }

                if (isLast) {
                    System.out.print("[" + symbol + "]");
                } else {
                    System.out.print(" " + symbol + " ");
                }
            }
            System.out.println(" " + r);
        }
        System.out.print("   ");
        System.out.println(new String(colLetters));
        System.out.println("-------------------------------------------------------------------------");
    }

    private String toGtpString(int row, int col) {
        char[] colLetters = "ABCDEFGHJKLMNOPQRST".toCharArray();
        char letter = colLetters[col];
        int gtpRow = boardSize - row;
        return "" + letter + gtpRow;
    }

    private boolean checkWin(byte[][] board, int row, int col, byte color) {
        int[] dx = {1, 0, 1, 1};
        int[] dy = {0, 1, 1, -1};
        for (int i = 0; i < 4; i++) {
            int count = 1;
            int r = row + dx[i], c = col + dy[i];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board[r][c] == color) {
                count++; r += dx[i]; c += dy[i];
            }
            r = row - dx[i]; c = col - dy[i];
            while (r >= 0 && r < boardSize && c >= 0 && c < boardSize && board[r][c] == color) {
                count++; r -= dx[i]; c -= dy[i];
            }
            if (count >= 5) return true;
        }
        return false;
    }
}