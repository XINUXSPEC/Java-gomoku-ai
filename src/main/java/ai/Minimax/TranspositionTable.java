package ai.Minimax;

import java.util.HashMap;

/**
 * 置换表（Transposition Table）
 * * 职责：
 * 1. 缓存已计算的棋盘状态评估结果
 * 2. 提供快速查询和存储功能
 * * 使用 Zobrist 哈希作为键，存储评估分数、搜索深度、边界标志及最佳着法
 */
public class TranspositionTable {

    // 定义边界标志的常量（底层逻辑：区分绝对分值与剪枝边界）
    public static final byte EXACT = 0;       // 精确值（完全搜索）
    public static final byte LOWERBOUND = 1;  // 下限值（发生 Beta 截断，真实分数 >= score）
    public static final byte UPPERBOUND = 2;  // 上限值（所有分支未突破 Alpha，真实分数 <= score）

    /** 置换表条目 */
    public static class TTEntry {
        public final double score;  // 评估分数
        public final int depth;     // 搜索深度
        public final byte flag;     // 边界标志 (EXACT, LOWERBOUND, UPPERBOUND)
        public final int bestMove;  // 最佳走法（可以使用 int 编码的坐标，如 row * borderSize + col，没有则存 -1）

        public TTEntry(double score, int depth, byte flag, int bestMove) {
            this.score = score;
            this.depth = depth;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }

    /** 哈希表 - 存储棋盘状态 -> 评估结果 */
    private final HashMap<Long, TTEntry> table;

    public TranspositionTable() {
        this.table = new HashMap<>();
    }

    /**
     * 查询置换表
     * @param hash 棋盘状态的 Zobrist 哈希值
     * @return 缓存的评估结果，如果未找到返回 null
     */
    public TTEntry get(long hash) {
        return table.get(hash);
    }

    /**
     * 存储评估结果到置换表（带深度优化覆盖策略）
     * * @param hash     棋盘状态的 Zobrist 哈希值
     * @param score    评估分数
     * @param depth    搜索深度
     * @param flag     边界标志
     * @param bestMove 最佳走法
     */
    public void put(long hash, double score, int depth, byte flag, int bestMove) {
        TTEntry existing = table.get(hash);

        // 覆盖策略（Depth-Preferred）：只有当新搜索的深度大于或等于已有的缓存深度时，才允许覆盖
        // 底层逻辑：高质量的深层搜索分绝对不能被浅层搜索产生的粗糙分破坏
        if (existing == null || depth >= existing.depth) {
            table.put(hash, new TTEntry(score, depth, flag, bestMove));
        }
    }

    /**
     * 清空置换表（新对局开始前必须调用）
     */
    public void clear() {
        table.clear();
    }

    public int size() {
        return table.size();
    }

    public boolean contains(long hash) {
        return table.containsKey(hash);
    }
}