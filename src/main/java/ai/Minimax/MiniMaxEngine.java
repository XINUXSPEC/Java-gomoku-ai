package ai.Minimax;

import   ai.AI_Engine;
import   utils.GameConstants;
import java.awt.*;

/**
 * Minimax 引擎 - 五子棋 AI 的核心算法（已修复版本）
 *
 * 关键修复：
 * 1. Killer move 截断时也调用 updateKiller，确保 killer 被正确传播
 * 2. TT move 截断时也调用 updateKiller，提升后续深度的 move ordering
 */
public class MiniMaxEngine implements AI_Engine {

    // ==================== 常量定义 ====================
    public static final int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};
    public static final int DEFAULT_MAX_DEPTH = 6;
    public static final long DEFAULT_TIME_LIMIT_MILLIS = 700;

    // ==================== 状态变量 ====================
    private byte aiColor;
    private byte playerColor;
    private int currentDepth = 5;
    private final int maxDepth;
    private final long timeLimitMillis;
    public byte[][] panel = new byte[GameConstants.ChessBorderNum][GameConstants.ChessBorderNum];

    // ==================== 功能模块 ====================
    private final TranspositionTable transpositionTable;  // 置换表
    private final ZobristHash zobristHash;              // Zobrist 哈希
    private final StaticLinkedList emptyCells;          // 空位列表
    private final EvaluatorRule evaluator;              // 评估函数

    // ==================== 搜索缓存 ====================
    private final int[][] killerMoves = new int[32][2];          // 杀手棋
    private final int[][] moveListPool = new int[32][StaticLinkedList.totalCells];  // 移动列表池
    private final int[][][] bucketPool = new int[32][6][230];    // Bucket池

    // ==================== 构造函数 ====================
    public MiniMaxEngine() {
        this(DEFAULT_MAX_DEPTH, DEFAULT_TIME_LIMIT_MILLIS);
    }

    public MiniMaxEngine(int maxDepth, long timeLimitMillis) {
        this.maxDepth = Math.max(2, maxDepth);
        this.timeLimitMillis = Math.max(200, timeLimitMillis);

        // 初始化功能模块
        this.transpositionTable = new TranspositionTable();
        this.zobristHash = new ZobristHash();
        this.emptyCells = new StaticLinkedList();
        this.evaluator = new EvaluatorRule();


    }

    // ==================== 公共接口 ====================
    @Override
    public Point findBestMove(byte[][] board) {
        initState(board);
        return getBestPosition(this.panel);
    }

    @Override
    public Point findBestMove(byte[][] board, Point lastMove) {
        // MiniMax 不需要上一手信息，直接调用原方法
        return findBestMove(board);
    }

    @Override
    public void  Init(byte flag){
        this.aiColor = flag;
        this.playerColor = (byte) (3 - flag);
        transpositionTable.clear();
        System.out.println("[MiniMaxEngine] 已初始化（清空置换表）");
    }

    // ==================== 初始化 ====================
    private void initState(byte[][] board) {
        // 初始化空位列表
        emptyCells.init();

        // 初始化杀手棋
        for (int i = 0; i < killerMoves.length; i++) {
            killerMoves[i][0] = killerMoves[i][1] = -1;
        }

        // 复制棋盘状态
        int border = GameConstants.ChessBorderNum;
        for (int i = 0; i < border; i++) {
            for (int j = 0; j < border; j++) {
                panel[i][j] = board[i][j];
                if (panel[i][j] != 0) {
                    emptyCells.removeCell(i, j);
                    emptyCells.addCellsAround(i, j, panel, Direction);
                }
            }
        }
    }

    // ==================== 主搜索方法 ====================
    public Point getBestPosition(byte[][] panel) {
        Point bestPoint = new Point(GameConstants.ChessBorderNum / 2, GameConstants.ChessBorderNum / 2);
        long startTime = System.currentTimeMillis();

        double baseScore = evaluator.evaluate(panel, aiColor, playerColor, Direction);

        for (int depth = 2; depth <= maxDepth; depth += 2) {
            currentDepth = depth;

            // 建议：只清空当前深度以下的
            for (int i = currentDepth; i < killerMoves.length; i++) {
                killerMoves[i][0] = killerMoves[i][1] = -1;
            }

            Point candidate = searchAtDepth(panel, baseScore, startTime);
            if (candidate != null) {
                bestPoint = candidate;
            }

            if (System.currentTimeMillis() - startTime > timeLimitMillis) {
                break;
            }
        }
        return bestPoint;
    }

    // ==================== 指定深度搜索 ====================
    public Point searchAtDepth(byte[][] panel, double baseScore, long startTime) {
        int mid = GameConstants.ChessBorderNum / 2;
        Point point = new Point(mid, mid);
        double alpha = Double.NEGATIVE_INFINITY;

        int[] keys = moveListPool[currentDepth];
        int sz = emptyCells.getEmptyPointsList(keys);

        if (sz == 0) return point;

        fillBuckets(panel, currentDepth, keys, sz);
        int[][] rootBuckets = bucketPool[currentDepth];

        // 计算初始哈希
        long rootHash = zobristHash.computeInitialHash(panel);

        for (int w = 5; w >= 1; w--) {
            int[] bucket = rootBuckets[w];
            int bucketSize = bucket[0];

            for (int k = 1; k <= bucketSize; k++) {
                int key = bucket[k];
                int i = key / GameConstants.ChessBorderNum;
                int j = key % GameConstants.ChessBorderNum;

                if (panel[i][j] != 0) continue;

                // 落子
                panel[i][j] = aiColor;
                emptyCells.removeCell(i, j);
                emptyCells.addCellsAround(i, j, panel, Direction);

                // 更新哈希
                long nextHash = zobristHash.updateHash(rootHash, i, j, aiColor);
                double moveScore = baseScore + evaluator.eval(panel, i, j, Direction);

                // 递归搜索
                double t = minimax(panel, currentDepth - 1, moveScore, alpha, Double.POSITIVE_INFINITY, false, nextHash);

                // 悔棋
                panel[i][j] = 0;
                emptyCells.removeCellsAround(i, j, panel, Direction);
                emptyCells.addCell(i, j);

                // 更新最佳点
                if (t > alpha) {
                    alpha = t;
                    point.x = i;
                    point.y = j;
                }

                // 超时检查
                if (System.currentTimeMillis() - startTime > timeLimitMillis + 200) {
                    return point;
                }
            }
        }
        return point;
    }

    // ====================  Minimax 核心算法 ====================
    public double minimax(byte[][] panel, int depth, double scorePre, double alpha, double beta,
                          boolean isMax, long hash) {

        // 保留最原始的 alpha 和 beta，用于末尾判断 Flag 类型
        double oldAlpha = alpha;
        double oldBeta = beta;

        // 1. 查表 - 分离"剪枝"和"排序"
        int ttBestMove = -1;  // 用于 move ordering 的候选
        TranspositionTable.TTEntry tt = transpositionTable.get(hash);

        if (tt != null) {
            // 即使深度不足，也记录 ttBestMove 用于排序
            ttBestMove = tt.bestMove;

            // 只有深度足够，才相信 TT 的分数用于 alpha-beta 决策
            if (tt.depth >= depth) {
                if (tt.flag == TranspositionTable.EXACT) {
                    return tt.score;  // 精确值，直接返回
                } else if (tt.flag == TranspositionTable.LOWERBOUND) {
                    alpha = Math.max(alpha, tt.score);
                } else if (tt.flag == TranspositionTable.UPPERBOUND) {
                    beta = Math.min(beta, tt.score);
                }

                if (alpha >= beta) {
                    return tt.score;  // 置换表剪枝
                }
            }
        }

        // 深度为0时返回评估分
        if (depth == 0) {
            return scorePre;
        }

        int[] moves = moveListPool[depth];
        int sz = emptyCells.getEmptyPointsList(moves);

        int killer0 = killerMoves[depth][0];
        int killer1 = killerMoves[depth][1];

        // 记录本次搜索产生的最佳走法
        int bestMoveKey = -1;

        if (isMax) {
            double score = Double.NEGATIVE_INFINITY;

            // ==================== 优先级 1: Killer Move 0 ====================
            if (killer0 != -1 && panel[killer0 / GameConstants.ChessBorderNum][killer0 % GameConstants.ChessBorderNum] == 0) {
                score = tryMove(panel, killer0, scorePre, alpha, beta, depth, isMax, hash, score);
                if (score > alpha) {
                    alpha = score;
                    bestMoveKey = killer0;
                }
                if (beta <= alpha) {
                    updateKiller(depth, killer0);  // ✓ 【修复】更新 killer
                    transpositionTable.put(hash, score, depth, TranspositionTable.LOWERBOUND, killer0);
                    return score;
                }
            }

            // ==================== 优先级 2: Killer Move 1 ====================
            if (killer1 != -1 && panel[killer1 / GameConstants.ChessBorderNum][killer1 % GameConstants.ChessBorderNum] == 0) {
                score = tryMove(panel, killer1, scorePre, alpha, beta, depth, isMax, hash, score);
                if (score > alpha) {
                    alpha = score;
                    bestMoveKey = killer1;
                }
                if (beta <= alpha) {
                    updateKiller(depth, killer1);  // ✓ 【修复】更新 killer
                    transpositionTable.put(hash, score, depth, TranspositionTable.LOWERBOUND, killer1);
                    return score;
                }
            }

            // ==================== 优先级 3: TT Move ====================
            if (ttBestMove != -1 && ttBestMove != killer0 && ttBestMove != killer1) {
                int tti = ttBestMove / GameConstants.ChessBorderNum;
                int ttj = ttBestMove % GameConstants.ChessBorderNum;
                if (panel[tti][ttj] == 0) {
                    score = tryMove(panel, ttBestMove, scorePre, alpha, beta, depth, isMax, hash, score);
                    if (score > alpha) {
                        alpha = score;
                        bestMoveKey = ttBestMove;
                    }
                    if (beta <= alpha) {
                        updateKiller(depth, ttBestMove);  // ✓ 【修复】更新 killer
                        transpositionTable.put(hash, score, depth, TranspositionTable.LOWERBOUND, ttBestMove);
                        return score;
                    }
                }
            }

            // ==================== 优先级 4: 权重排序 ====================
            fillBuckets(panel, depth, moves, sz);
            int[][] layerBuckets = bucketPool[depth];

            for (int w = 5; w >= 1; w--) {
                int[] bucket = layerBuckets[w];
                int bucketSize = bucket[0];

                for (int k = 1; k <= bucketSize; k++) {
                    int key = bucket[k];

                    // 跳过已经搜索过的 killer 和 TT move
                    if (key == killer0 || key == killer1 || key == ttBestMove) {
                        continue;
                    }

                    int i = key / GameConstants.ChessBorderNum;
                    int j = key % GameConstants.ChessBorderNum;

                    if (panel[i][j] != 0) continue;

                    score = tryMove(panel, key, scorePre, alpha, beta, depth, isMax, hash, score);
                    if (score > alpha) {
                        alpha = score;
                        bestMoveKey = key;
                    }

                    if (beta <= alpha) {
                        updateKiller(depth, key);
                        transpositionTable.put(hash, score, depth, TranspositionTable.LOWERBOUND, key);
                        return score;
                    }
                }
            }

            // 记录本轮搜索结果到 TT
            byte flag = (score <= oldAlpha) ? TranspositionTable.UPPERBOUND :
                    (score >= oldBeta)  ? TranspositionTable.LOWERBOUND : TranspositionTable.EXACT;
            transpositionTable.put(hash, score, depth, flag, bestMoveKey);
            return score;

        } else {
            // ==================== MIN 层逻辑完全对称 ====================
            double score = Double.POSITIVE_INFINITY;

            // 优先级 1: Killer Move 0
            if (killer0 != -1 && panel[killer0 / GameConstants.ChessBorderNum][killer0 % GameConstants.ChessBorderNum] == 0) {
                score = tryMove(panel, killer0, scorePre, alpha, beta, depth, isMax, hash, score);
                if (score < beta) {
                    beta = score;
                    bestMoveKey = killer0;
                }
                if (beta <= alpha) {
                    updateKiller(depth, killer0);  // ✓ 【修复】更新 killer
                    transpositionTable.put(hash, score, depth, TranspositionTable.UPPERBOUND, killer0);
                    return score;
                }
            }

            // 优先级 2: Killer Move 1
            if (killer1 != -1 && panel[killer1 / GameConstants.ChessBorderNum][killer1 % GameConstants.ChessBorderNum] == 0) {
                score = tryMove(panel, killer1, scorePre, alpha, beta, depth, isMax, hash, score);
                if (score < beta) {
                    beta = score;
                    bestMoveKey = killer1;
                }
                if (beta <= alpha) {
                    updateKiller(depth, killer1);  // ✓ 【修复】更新 killer
                    transpositionTable.put(hash, score, depth, TranspositionTable.UPPERBOUND, killer1);
                    return score;
                }
            }

            // 优先级 3: TT Move
            if (ttBestMove != -1 && ttBestMove != killer0 && ttBestMove != killer1) {
                int tti = ttBestMove / GameConstants.ChessBorderNum;
                int ttj = ttBestMove % GameConstants.ChessBorderNum;
                if (panel[tti][ttj] == 0) {
                    score = tryMove(panel, ttBestMove, scorePre, alpha, beta, depth, isMax, hash, score);
                    if (score < beta) {
                        beta = score;
                        bestMoveKey = ttBestMove;
                    }
                    if (beta <= alpha) {
                        updateKiller(depth, ttBestMove);  // ✓ 【修复】更新 killer
                        transpositionTable.put(hash, score, depth, TranspositionTable.UPPERBOUND, ttBestMove);
                        return score;
                    }
                }
            }

            // 优先级 4: 权重排序
            fillBuckets(panel, depth, moves, sz);
            int[][] layerBuckets = bucketPool[depth];

            for (int w = 5; w >= 1; w--) {
                int[] bucket = layerBuckets[w];
                int bucketSize = bucket[0];

                for (int k = 1; k <= bucketSize; k++) {
                    int key = bucket[k];

                    if (key == killer0 || key == killer1 || key == ttBestMove) {
                        continue;
                    }

                    int i = key / GameConstants.ChessBorderNum;
                    int j = key % GameConstants.ChessBorderNum;

                    if (panel[i][j] != 0) continue;

                    score = tryMove(panel, key, scorePre, alpha, beta, depth, isMax, hash, score);
                    if (score < beta) {
                        beta = score;
                        bestMoveKey = key;
                    }

                    if (beta <= alpha) {
                        updateKiller(depth, key);
                        transpositionTable.put(hash, score, depth, TranspositionTable.UPPERBOUND, key);
                        return score;
                    }
                }
            }

            // 记录本轮搜索结果到 TT
            byte flag = (score <= oldAlpha) ? TranspositionTable.UPPERBOUND :
                    (score >= oldBeta)  ? TranspositionTable.LOWERBOUND : TranspositionTable.EXACT;
            transpositionTable.put(hash, score, depth, flag, bestMoveKey);
            return score;
        }
    }

    // ==================== 辅助方法：执行一步走法并递归搜索 ====================
    private double tryMove(byte[][] panel, int moveKey, double scorePre, double alpha, double beta,
                           int depth, boolean isMax, long hash, double currentScore) {

        int i = moveKey / GameConstants.ChessBorderNum;
        int j = moveKey % GameConstants.ChessBorderNum;

        byte color = isMax ? aiColor : playerColor;

        // 落子
        panel[i][j] = color;
        emptyCells.removeCell(i, j);
        emptyCells.addCellsAround(i, j, panel, Direction);

        // 递归搜索
        double delta = evaluator.eval(panel, i, j, Direction);
        long nextHash = zobristHash.updateHash(hash, i, j, color);
        double score = isMax
                ? minimax(panel, depth - 1, scorePre + delta, alpha, beta, false, nextHash)
                : minimax(panel, depth - 1, scorePre - delta, alpha, beta, true, nextHash);

        // 恢复
        panel[i][j] = 0;
        emptyCells.removeCellsAround(i, j, panel, Direction);
        emptyCells.addCell(i, j);

        // 更新分数
        return isMax ? Math.max(currentScore, score) : Math.min(currentScore, score);
    }

    // ==================== 更新杀手棋 ====================
    public void updateKiller(int depth, int key) {
        if (killerMoves[depth][0] != key) {
            killerMoves[depth][1] = killerMoves[depth][0];
            killerMoves[depth][0] = key;
        }
    }

    /**
     * 启发式动作排序优化：将当前深度的所有合法落子进行分类与分级（桶排序思想）。
     * 核心优化点：
     * 1. 空间预分配：直接复用预先分配好的 bucketPool[depth]，运行时零对象创建，避免触发 GC。
     * 2. 剪枝最大化：优先将高威胁、高战略价值的落子放入高级别桶中，使得 Alpha-Beta 算法能优先搜索最优分支，极大提升剪枝率。
     *
     * @param panel  当前棋盘状态
     * @param depth  当前搜索树的深度
     * @param moves  待排序的合法落子集合（编码后的 key 数组）
     * @param sz     合法落子的有效数量
     * @return       返回处理的动作数量
     */
    public int fillBuckets(byte[][] panel, int depth, int[] moves, int sz) {
        // 【底层优化：空间预分配】直接复用当前深度对应的预分配桶内存，拒绝动态分配
        int[][] layerBuckets = bucketPool[depth];

        // 初始化/清空每个桶的计数器（第0个元素存储该桶当前装入的元素个数）
        for (int i = 1; i <= 5; i++) layerBuckets[i][0] = 0;

        // 遍历当前深度所有的合法落子进行“嗅探与分桶”
        for (int i = 0; i < sz; i++) {
            int key = moves[i];

            // 位运算/算术解包：将一维的 key 还原为棋盘的二维坐标 (x, y)
            int x = key / GameConstants.ChessBorderNum;
            int y = key % GameConstants.ChessBorderNum;

            // 【算法优化：快速局势嗅探】
            // 采用低消耗的局部扫描（Quick Sniff），分别评估我方和敌方在该位置的局部潜在棋型级别
            int self = evaluator.quickSniffByColor(panel, x, y, aiColor, Direction);
            int opp = evaluator.quickSniffByColor(panel, x, y, playerColor, Direction);
            int bucketIdx;

            // 【启发式分级策略】根据局部威胁程度决定该落子的搜索优先级（桶索引）
            if (self >= 5 || opp >= 5) bucketIdx = 5;       // 存在绝杀或致命威胁，直接进最高优先级桶
            else if (self >= 4 || opp >= 4) bucketIdx = 5;  // 冲四/活四点，必须优先搜索或防守
            else if (self == 3 && opp == 3) bucketIdx = 5;  // 双重活三爆发点
            else if (self == 3 || opp == 3) bucketIdx = 4;  // 单方活三点（二级高优）
            else if (self == 2 || opp == 2) bucketIdx = 3;  // 常规连二点
            else bucketIdx = 1;                             // 价值较低的普通落子

            if (EvaluatorRule.centerDistanceWeightTable[x][y] > 30 && bucketIdx < 5) {
                bucketIdx++;
            }

            // 【无锁/零开销入桶】自增当前桶计数器，并将落子的 key 存入对应桶的空闲槽位中
            int idx = ++layerBuckets[bucketIdx][0];
            layerBuckets[bucketIdx][idx] = key;
        }
        return sz;
    }
}