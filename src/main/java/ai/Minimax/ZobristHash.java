package ai.Minimax;

import java.util.Random;

/**
 * Zobrist 哈希生成器
 * * 职责：
 * 1. 生成和管理 Zobrist 随机数表
 * 2. 提供哈希值的获取和更新功能
 * * 优化：移除显式回合 Hash。在五子棋中，行动方由盘面子数直接决定。
 */
public class ZobristHash {

    /** Zobrist 随机数表 - zobrist[x][y][color] */
    private final long[][][] zobristTable;

    /** 棋盘大小 */
    private static final int BOARD_SIZE = 15;

    /** 颜色数量（0=空，1=黑，2=白） */
    private static final int COLOR_COUNT = 3;

    public ZobristHash() {
        this.zobristTable = new long[BOARD_SIZE][BOARD_SIZE][COLOR_COUNT];

        // 底层逻辑提示：不要使用默认的带有时间戳的 Random，为了避免极其罕见的哈希碰撞，
        // 可以使用强随机数（或固定种子便于 Debug），这里沿用你的设计
        Random r = new Random();

        // 为每个位置、每种颜色生成随机数
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                zobristTable[i][j][1] = r.nextLong();  // 黑棋
                zobristTable[i][j][2] = r.nextLong();  // 白棋
            }
        }
    }

    /**
     * 计算初始棋盘哈希值
     * @param board 棋盘状态
     * @return 初始哈希值
     */
    public long computeInitialHash(byte[][] board) {
        long hash = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) {
                    hash ^= zobristTable[i][j][board[i][j]];
                }
            }
        }
        return hash;
    }

    /**
     * 更新哈希值（落子/撤销落子）
     * * @param currentHash 当前哈希值
     * @param x 落子位置X坐标
     * @param y 落子位置Y坐标
     * @param color 棋子颜色
     * @return 更新后的哈希值
     */
    public long updateHash(long currentHash, int x, int y, int color) {
        return currentHash ^ zobristTable[x][y][color];
    }
}