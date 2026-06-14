import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.BoardUtils;
import utils.GameConstants;

import java.awt.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestMinimax {

    static class PointNode {
        public int Key;       // 使用 (x << 4) | y
        public int NextIdx;
        public int PrevIdx;
    }

    static class TTEntry {
        double score;
        int depth;

        TTEntry(double score, int depth) {
            this.score = score;
            this.depth = depth;
        }
    }

    static public HashMap<Integer, Integer> activePointsMap = new HashMap<>();
    public final static int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};

    static public byte AI_COLOR = 2;
    static public byte PLAYER_COLOR = 1;
    static public int size = GameConstants.ChessBorderNum; // 15
    static double CenterWeight = 0.80;
    static double CenterScore = 40;

    public final static int border = GameConstants.ChessBorderNum; // 15

    // ==================== 位运算常量 ====================
    static final int SHIFT = 4;
    static final int MASK = 15;
    static final int BASE = 16;
    static int totalCells = BASE * BASE; // 256
    public static final int MAX_DEPTH = 10;
    public int currentDepth = 5;
    static byte AI = AI_COLOR;
    static byte Player = (byte) (AI == 1 ? 2 : 1);

    static int[][] KillerMoves = new int[32][2];
    static int[][] MoveListPool = new int[32][totalCells];
    static int CellSize = totalCells;
    static PointNode[] CellsListPool = new PointNode[CellSize + 2];
    static final int HEAD = CellSize, TAIL = CellSize + 1;

    public byte[][] panel = new byte[border][border];
    public int[] CellsCnt = new int[totalCells];

    public static final double[][] centerDistanceWeightTable = new double[GameConstants.ChessBorderNum][GameConstants.ChessBorderNum];

    static int MinimaxCnt = 0;
    static int[][][] BucketPool = new int[32][6][230];
    static long[][][] Zobrist = new long[15][15][3];
    static HashMap<Long, TTEntry> TT = new HashMap<>();

    static long SIDE_HASH;

    static {
        int mid = GameConstants.ChessBorderNum / 2;
        for (int r = 0; r < GameConstants.ChessBorderNum; r++) {
            for (int c = 0; c < GameConstants.ChessBorderNum; c++) {
                double distance = Math.sqrt((r - mid) * (r - mid) + (c - mid) * (c - mid));
                centerDistanceWeightTable[r][c] = CenterScore - CenterScore * (1 - CenterWeight) * distance;
            }
        }
        Random r = new Random();
        for(int i=0;i<15;i++){
            for(int j=0;j<15;j++){
                Zobrist[i][j][1] = r.nextLong();
                Zobrist[i][j][2] = r.nextLong();
            }
        }
        SIDE_HASH = r.nextLong();
        for (int i = 0; i < CellsListPool.length; i++) {
            CellsListPool[i] = new PointNode();
            CellsListPool[i].Key = i;
        }
    }
    public void Init() {
        InitCellListPool();

        // 彻底清空上一局残留的杀手步历史缓存，防止脏数据污染新一轮搜索
        for (int i = 0; i < KillerMoves.length; i++) {
            KillerMoves[i][0] = -1;
            KillerMoves[i][1] = -1;
        }
    }


    public Point getBestPosition(byte[][] panel) {
        Point bestPoint = new Point(0, 0);
        long startTime = System.currentTimeMillis();
        MinimaxCnt = 0;

        double baseScore = evaluate(panel, AI);

        for (int depth = 2; depth <= MAX_DEPTH; depth += 2) {
            currentDepth = depth;
            MinimaxCnt = 0;

            // 每层迭代加深依然保持清空，防止浅层边界数据误剪枝
            TT.clear();
            for (int i = 0; i < KillerMoves.length; i++) {
                KillerMoves[i][0] = KillerMoves[i][1] = -1;
            }

            Point candidate = searchAtDepth(panel, baseScore, startTime);
            long usedTime = System.currentTimeMillis() - startTime;

            if (candidate != null) {
                bestPoint = candidate;
            }

            System.out.println("Depth " + depth + " completed, nodes: " + MinimaxCnt + ", time: " + usedTime + "ms");
            if (usedTime > 650) {
                break;
            }
        }

        System.out.println("最终最佳落子: (" + bestPoint.x + ", " + bestPoint.y + ")");
        return bestPoint;
    }

    // ====================== 2. searchAtDepth 完整替换版本 ======================
    public Point searchAtDepth(byte[][] panel, double baseScore, long startTime) {
        int mid = size / 2;
        Point point = new Point(mid, mid);
        double alpha = Double.NEGATIVE_INFINITY;

        int[] Keys = MoveListPool[currentDepth];
        int sz = GetEmptyPointsList(panel, Keys);

        if (sz == 0) {
            return point;
        }

        fillBuckets(panel, currentDepth, Keys, sz);
        int[][] rootBuckets = BucketPool[currentDepth];

        long rootHash = 0;
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                if (panel[i][j] != 0) {
                    rootHash ^= Zobrist[i][j][panel[i][j]];
                }
            }
        }

        boolean foundAny = false;

        for (int w = 5; w >= 1; w--) {
            int[] bucket = rootBuckets[w];
            int bucketSize = bucket[0];

            for (int k = 1; k <= bucketSize; k++) {
                int key = bucket[k];
                int i = key >>> SHIFT;
                int j = key & MASK;

                if (panel[i][j] != 0) continue;
                foundAny = true;

                panel[i][j] = AI;
                SaveActiveChess(i, j);

                long nextHash = rootHash ^ Zobrist[i][j][AI] ^ SIDE_HASH;
                double moveScore = baseScore + eval(panel, i, j);

                double t = minimax(
                        panel,
                        currentDepth - 1,
                        moveScore,
                        alpha,
                        Double.POSITIVE_INFINITY,
                        false,
                        nextHash
                );

                panel[i][j] = 0;
                RemoveActiveChess(i, j);

                if (t > alpha) {
                    alpha = t;
                    point.x = i;
                    point.y = j;
                }

                if (System.currentTimeMillis() - startTime > 900) {
                    return point;
                }
            }
        }

        if (!foundAny) {
            return new Point(mid, mid);
        }

        return point;
    }

    // ====================== 3. minimax 完整替换版本 ======================
    public double minimax(
            byte[][] panel,
            int depth,
            double ScorePre,
            double alh,
            double Beta,
            boolean isMax,
            long hash
    ) {
        MinimaxCnt++;
        // 1. 既然 depth 永远 > 0，TT 缓存可以无脑读取，不需要加判断
        TTEntry tt = TT.get(hash);
        if (tt != null && tt.depth >= depth) {
            return tt.score;
        }

        // 3. 【核心改动】当主搜索深度耗尽（depth == 0），立刻无缝移交战棒给静态搜索
        if (depth == 0) {
            // 传入一个最大延伸计数器（比如 4），代表最多允许延伸 4 层物理层
            return ScorePre;
        }

        int[] moves = MoveListPool[depth];
        int sz = GetEmptyPointsList(panel, moves);

        int killer0 = KillerMoves[depth][0];
        int killer1 = KillerMoves[depth][1];

        if (isMax) {
            double score = Double.NEGATIVE_INFINITY;

            // 尝试杀手步 0（内部已修复顺序）
            score = tryKillerMove(panel, killer0, ScorePre, alh, Beta, depth, true, score, hash);
            if (score > alh) alh = score;
            if (Beta <= alh) return score;

            // 尝试杀手步 1（内部已修复顺序）
            score = tryKillerMove(panel, killer1, ScorePre, alh, Beta, depth, true, score, hash);
            if (score > alh) alh = score;
            if (Beta <= alh) return score;

            fillBuckets(panel, depth, moves, sz);
            int[][] layerBuckets = BucketPool[depth];

            for (int w = 5; w >= 1; w--) {
                int[] bucket = layerBuckets[w];
                int bucketSize = bucket[0];

                for (int k = 1; k <= bucketSize; k++) {
                    int key = bucket[k];
                    if (key == killer0 || key == killer1) continue;

                    int i = key >>> SHIFT;
                    int j = key & MASK;

                    if (panel[i][j] != 0) continue;

                    // 【严格对齐】1. 先物理落子
                    panel[i][j] = AI;
                    SaveActiveChess(i, j);

                    // 【严格对齐】2. 后动态算分
                    double delta = eval(panel, i, j);

                    long nextHash = hash;
                    nextHash ^= Zobrist[i][j][AI];
                    nextHash ^= SIDE_HASH;

                    double t = minimax(
                            panel,
                            depth - 1,
                            ScorePre + delta,
                            alh,
                            Beta,
                            false,
                            nextHash
                    );

                    // 3. 回溯还原
                    panel[i][j] = 0;
                    RemoveActiveChess(i, j);

                    if (t > score) score = t;
                    if (score > alh) alh = score;

                    if (Beta <= alh) {
                        updateKiller(depth, key);
                        return score;
                    }
                }
            }
            TT.put(hash, new TTEntry(score, depth));
            return score;

        } else {
            double score = Double.POSITIVE_INFINITY;

            // 尝试杀手步 0
            score = tryKillerMove(panel, killer0, ScorePre, alh, Beta, depth, false, score, hash);
            if (score < Beta) Beta = score;
            if (Beta <= alh) return score;

            // 尝试杀手步 1
            score = tryKillerMove(panel, killer1, ScorePre, alh, Beta, depth, false, score, hash);
            if (score < Beta) Beta = score;
            if (Beta <= alh) return score;

            fillBuckets(panel, depth, moves, sz);
            int[][] layerBuckets = BucketPool[depth];

            for (int w = 5; w >= 1; w--) {
                int[] bucket = layerBuckets[w];
                int bucketSize = bucket[0];

                for (int k = 1; k <= bucketSize; k++) {
                    int key = bucket[k];
                    if (key == killer0 || key == killer1) continue;

                    int i = key >>> SHIFT;
                    int j = key & MASK;

                    if (panel[i][j] != 0) continue;

                    // 【严格对齐】1. 先物理落子
                    panel[i][j] = Player;
                    SaveActiveChess(i, j);

                    // 【严格对齐】2. 后动态算分
                    double delta = eval(panel, i, j);

                    long nextHash = hash;
                    nextHash ^= Zobrist[i][j][Player];
                    nextHash ^= SIDE_HASH;

                    double t = minimax(
                            panel,
                            depth - 1,
                            ScorePre - delta,
                            alh,
                            Beta,
                            true,
                            nextHash
                    );

                    // 3. 回溯还原
                    panel[i][j] = 0;
                    RemoveActiveChess(i, j);

                    if (t < score) score = t;
                    if (score < Beta) Beta = score;

                    if (Beta <= alh) {
                        updateKiller(depth, key);
                        return score;
                    }
                }
            }
            TT.put(hash, new TTEntry(score, depth));
            return score;
        }
    }

    // ====================== 4. tryKillerMove 完整替换版本 ======================
    public double tryKillerMove(
            byte[][] panel,
            int killer,
            double ScorePre,
            double alh,
            double Beta,
            int depth,
            boolean isMax,
            double currentBest,
            long hash
    ) {
        if (killer < 0) return currentBest;

        int i = killer >>> SHIFT;
        int j = killer & MASK;

        if (panel[i][j] != 0) return currentBest;

        byte color = isMax ? AI : Player;

        // 【核心修复】必须先在物理棋盘上落子并更新边缘活跃点
        panel[i][j] = color;
        SaveActiveChess(i, j);

        // 【核心修复】此时 panel[i][j] 已经有色，eval 才能正确识别 flag 颜色并进行算分
        double delta = isMax ? eval(panel, i, j) : -eval(panel, i, j);

        long nextHash = hash;
        nextHash ^= Zobrist[i][j][color];
        nextHash ^= SIDE_HASH;

        double t = minimax(
                panel,
                depth - 1,
                ScorePre + delta, // 此时传入的增量绝对精准
                alh,
                Beta,
                !isMax,
                nextHash
        );

        // 严谨回溯状态机
        panel[i][j] = 0;
        RemoveActiveChess(i, j);

        if (isMax) {
            if (t > currentBest) {
                currentBest = t;
            }
        } else {
            if (t < currentBest) {
                currentBest = t;
            }
        }

        return currentBest;
    }

    public void updateKiller(int depth, int key) {
        if (KillerMoves[depth][0] != key) {
            KillerMoves[depth][1] = KillerMoves[depth][0];
            KillerMoves[depth][0] = key;
        }
    }

    // ====================== 3) 替换你的 fillBuckets ======================
    public int fillBuckets(byte[][] panel, int depth, int[] moves, int sz) {
        int[][] layerBuckets = BucketPool[depth];
        for (int i = 1; i <= 5; i++) layerBuckets[i][0] = 0;

        for (int i = 0; i < sz; i++) {
            int key = moves[i];
            int x = key >>> SHIFT;
            int y = key & MASK;

            int self = quickSniffByColor(panel, x, y, AI);
            int opp = quickSniffByColor(panel, x, y, Player);
            int bucketIdx;

            if (self >= 5 || opp >= 5) bucketIdx = 5;
            else if (self >= 4 || opp >= 4) bucketIdx = 5;
            else if (self == 3 && opp == 3) bucketIdx = 5;
            else if (self == 3 || opp == 3) bucketIdx = 4;
            else if (self == 2 || opp == 2) bucketIdx = 3;
            else bucketIdx = 1;

            if (centerDistanceWeightTable[x][y] > 30 && bucketIdx < 5) bucketIdx++;

            int idx = ++layerBuckets[bucketIdx][0];
            layerBuckets[bucketIdx][idx] = key;
        }
        return sz;
    }

    public void SaveActiveChess(int x, int y) {
        DelOnCellsListPool(x, y);
        AddOnCellsAround(x, y);
    }

    public void RemoveActiveChess(int x, int y) {
        DelOnCellsAround(x, y);
        AddOnCellsListPool(x, y);
    }

    void InitCellListPool() {
        for (int i = 0; i < CellsListPool.length; i++) {
            CellsListPool[i].NextIdx = CellsListPool[i].PrevIdx = -1;
        }
        CellsListPool[HEAD].NextIdx = TAIL;
        CellsListPool[TAIL].NextIdx = -1;
        CellsListPool[HEAD].PrevIdx = -1;
        CellsListPool[TAIL].PrevIdx = HEAD;
    }

    public void DelOnCellsListPool(int x, int y) {
        int key = (x << SHIFT) | y;
        if (CellsListPool[key].PrevIdx != -1 || CellsListPool[key].NextIdx != -1) {
            CellsListPool[CellsListPool[key].PrevIdx].NextIdx = CellsListPool[key].NextIdx;
            CellsListPool[CellsListPool[key].NextIdx].PrevIdx = CellsListPool[key].PrevIdx;
            CellsListPool[key].PrevIdx = CellsListPool[key].NextIdx = -1;
        }
    }

    public void AddOnCellsListPool(int x, int y) {
        int key = (x << SHIFT) | y;
        if (CellsListPool[key].NextIdx == -1 || CellsListPool[key].PrevIdx == -1) {
            CellsListPool[key].PrevIdx = HEAD;
            CellsListPool[key].NextIdx = CellsListPool[HEAD].NextIdx;
            CellsListPool[CellsListPool[HEAD].NextIdx].PrevIdx = key;
            CellsListPool[HEAD].NextIdx = key;
        }
    }

    public void AddOnCellsAround(int x, int y) {
        for (int i = 0; i < 4; i++) {
            int dx = Direction[i][0], dy = Direction[i][1];
            for (int j = 1; j <= 2; j++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int tx = x + j * dx * sign;
                    int ty = y + j * dy * sign;
                    if (BoardUtils.IsLegal(tx, ty) && panel[tx][ty] == 0) {
                        int key = (tx << SHIFT) | ty;
                        if (CellsCnt[key] == 0) {
                            AddOnCellsListPool(tx, ty);
                        }
                        CellsCnt[key]++;
                    }
                }
            }
        }
    }

    public void DelOnCellsAround(int x, int y) {
        for (int i = 0; i < 4; i++) {
            int dx = Direction[i][0], dy = Direction[i][1];
            for (int j = 1; j <= 2; j++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int tx = x + j * dx * sign;
                    int ty = y + j * dy * sign;
                    if (BoardUtils.IsLegal(tx, ty)) {
                        int key = (tx << SHIFT) | ty;
                        if (panel[tx][ty] == 0 && CellsCnt[key] > 0) {
                            CellsCnt[key]--;
                            if (CellsCnt[key] == 0) {
                                DelOnCellsListPool(tx, ty);
                            }
                        }
                    }
                }
            }
        }
    }

    static public int GetEmptyPointsList(byte[][] panel, int[] res) {
        PointNode node = CellsListPool[CellsListPool[HEAD].NextIdx];
        int sz = 0;
        while (node.NextIdx != -1) {
            res[sz++] = node.Key;
            node = CellsListPool[node.NextIdx];
        }
        return sz;
    }

    double evaluate(byte[][] panel, byte AI) {
        double eval1 = 0, eval2 = 0;
        byte PlayerColor = (byte) (AI == 1 ? 2 : 1);
        int row = GameConstants.ChessBorderNum - 1;
        for (int i = 0; i <= row; i++) {
            for (int j = 0; j <= row; j++) {
                byte p = panel[i][j];
                if (p != PlayerColor && p != AI) continue;
                double t = eval(panel, i, j);
                if (p == PlayerColor) eval1 += t;
                else eval2 += t;
            }
        }
        return eval2 - eval1;
    }

    double eval(byte[][] panel, int x, int y) {
        byte flag = panel[x][y];
        double score = 0;
        int rule3 = 0;
        int rule4 = 0;

        for (int d = 0; d < 4; d++) {
            int dx = Direction[d][0];
            int dy = Direction[d][1];

            int count = 1;
            int leftOpen = 0;
            int rightOpen = 0;

            int cx = x + dx, cy = y + dy;
            while (isValid(cx, cy) && panel[cx][cy] == flag) {
                count++;
                cx += dx;
                cy += dy;
            }
            if (isValid(cx, cy) && panel[cx][cy] == 0) rightOpen = 1;

            cx = x - dx;
            cy = y - dy;
            while (isValid(cx, cy) && panel[cx][cy] == flag) {
                count++;
                cx -= dx;
                cy -= dy;
            }
            if (isValid(cx, cy) && panel[cx][cy] == 0) leftOpen = 1;

            int patternScore = getPatternScore(count, leftOpen, rightOpen);
            score += patternScore;

            boolean isLive = (leftOpen == 1 && rightOpen == 1);
            if (count == 4 && isLive) rule4++;
            else if (count == 3 && isLive) rule3++;

            if (count == 4 && (leftOpen + rightOpen == 1)) {
                score += 28000;
            }
        }

        if (rule4 >= 2) {
            score += 85000;
        } else if (rule4 == 1 && rule3 >= 1) {
            score += 32000;
        } else if (rule3 >= 2) {
            score += 15000;
        } else if (rule3 == 1 && rule4 == 1) {
            score += 18000;
        }

        score += centerDistanceWeightTable[x][y] * 1.1;
        return score;
    }

    public boolean isValid(int x, int y) {
        return x >= 0 && x < GameConstants.ChessBorderNum
                && y >= 0 && y < GameConstants.ChessBorderNum;
    }

    public int getPatternScore(int count, int leftOpen, int rightOpen) {
        boolean live = (leftOpen == 1 && rightOpen == 1);
        int openSides = leftOpen + rightOpen;

        if (count >= 5) return 1200000;

        switch (count) {
            case 4:
                return live ? 65000 : 28000;
            case 3:
                return live ? 7200 : 800;
            case 2:
                return live ? 280 : 40;
            case 1:
                return openSides > 0 ? 12 : 0;
            default:
                return 0;
        }
    }

    public int quickSniffByColor(byte[][] panel, int x, int y, byte color) {
        int maxSeq = 0;
        for (int d = 0; d < 4; d++) {
            int dx = Direction[d][0], dy = Direction[d][1];
            int cnt = 1;

            for (int j = 1; j <= 4; j++) {
                int tx = x + dx * j, ty = y + dy * j;
                if (!isValid(tx, ty)) break;
                if (panel[tx][ty] == color) cnt++;
                else break;
            }
            for (int j = 1; j <= 4; j++) {
                int tx = x - dx * j, ty = y - dy * j;
                if (!isValid(tx, ty)) break;
                if (panel[tx][ty] == color) cnt++;
                else break;
            }
            maxSeq = Math.max(maxSeq, cnt);
            if (maxSeq >= 5) return 5;
        }
        return maxSeq;
    }
    // ====================== JUNIT 测试用例 ======================

    @Test
    @DisplayName("测试：当棋盘有落子时，AI能够通过Minimax计算并返回一个合法的最佳落子位置")
    void testGetBestPositionWithActivePoints() {
        InitCellListPool();
        activePointsMap.clear();

        panel[7][7] = PLAYER_COLOR; SaveActiveChess(7, 7);

        Point bestPos = getBestPosition(panel);

        assertNotNull(bestPos);
        assertTrue(bestPos.x >= 0 && bestPos.x < size);
        assertTrue(bestPos.y >= 0 && bestPos.y < size);
        assertEquals(0, panel[bestPos.x][bestPos.y]);
    }

    @Test
    @DisplayName("测试：AI 进攻（活四）- 应该直接选择获胜点（成五）")
    void testAIAttackWin() {
        InitCellListPool();
        activePointsMap.clear();

        int testRow = 3;
        for (int col = 1; col <= 4; col++) {
            panel[testRow][col] = AI_COLOR; SaveActiveChess(testRow, col);
        }

        Point bestPos = getBestPosition(panel);
        assertTrue((bestPos.x == testRow && bestPos.y == 5) || (bestPos.x == testRow && bestPos.y == 0));
    }

    @Test
    @DisplayName("测试：AI 防守 - 玩家有活三/活四时，AI 应前往拦截")
    void testAIDefend() {
        InitCellListPool();
        activePointsMap.clear();

        int[][] positions = {{2, 2}, {3, 3}, {4, 4}};
        for (int[] pos : positions) {
            panel[pos[0]][pos[1]] = PLAYER_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        panel[6][4] = AI_COLOR; SaveActiveChess(6, 4);

        Point bestPos = getBestPosition(panel);
        System.out.println("AI 拦截位置: " + bestPos);

        assertTrue((bestPos.x == 1 && bestPos.y == 1) || (bestPos.x == 5 && bestPos.y == 5));
    }

    @Test
    @DisplayName("测试：满棋盘无处落子（极端边界情况）")
    void testFullPanel() {
        InitCellListPool();
        activePointsMap.clear();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                panel[i][j] = PLAYER_COLOR; SaveActiveChess(i, j);
            }
        }

        Point bestPos = getBestPosition(panel);
        assertEquals(0, bestPos.x);
        assertEquals(0, bestPos.y);
    }

    @Test
    @DisplayName("测试：双三/四三进攻抉择 - 存在获胜点时，AI是否能识别并落子")
    void testDoubleThreeAttack() {
        InitCellListPool();
        activePointsMap.clear();

        int[][] aiPositions = {{7, 5}, {7, 6}, {7, 7}, {6, 7}, {5, 7}};
        for (int[] pos : aiPositions) {
            panel[pos[0]][pos[1]] = AI_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        Point bestPos = getBestPosition(panel);
        boolean isValidAttack = (bestPos.x == 7 && bestPos.y == 8) ||
                (bestPos.x == 7 && bestPos.y == 4) ||
                (bestPos.x == 4 && bestPos.y == 7) ||
                (bestPos.x == 8 && bestPos.y == 7);

        assertTrue(isValidAttack);
    }

    @Test
    @DisplayName("测试：防守优先级 - 玩家同时有活三和活二，AI 应优先堵活三")
    void testDefensePriority() {
        activePointsMap.clear();
        InitCellListPool();

        int[][] t1 = {{2, 3}, {2, 4}, {2, 5}};
        for (int[] pos : t1) {
            panel[pos[0]][pos[1]] = PLAYER_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        int[][] t2 = {{8, 8}, {8, 9}};
        for (int[] pos : t2) {
            panel[pos[0]][pos[1]] = PLAYER_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        Point bestPos = getBestPosition(panel);
        assertTrue(bestPos.x == 2 && (bestPos.y == 2 || bestPos.y == 6));
    }

    @Test
    @DisplayName("测试：死四 vs 活三 - 评估函数对受堵棋局的判分逻辑")
    void testBlockedFourVsActiveThree() {
        activePointsMap.clear();
        InitCellListPool();

        panel[4][2] = AI_COLOR; SaveActiveChess(4, 2);

        int[][] playerPos = {{4, 3}, {4, 4}, {4, 5}, {4, 6}};
        for (int[] pos : playerPos) {
            panel[pos[0]][pos[1]] = PLAYER_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        Point bestPos = getBestPosition(panel);
        assertEquals(4, bestPos.x);
        assertEquals(7, bestPos.y);
    }

    @Test
    @DisplayName("测试：性能与耗时 - 评估单次决策的计算效率")
    void testPerformanceTime() {
        activePointsMap.clear();
        InitCellListPool();

        for (int i = 0; i < size; i += 2) {
            for (int j = 0; j < size; j += 3) {
                panel[i][j] = (byte) ((i + j) % 2 == 0 ? AI_COLOR : PLAYER_COLOR);
                SaveActiveChess(i, j);
            }
        }

        long startTime = System.currentTimeMillis();
        Point bestPos = getBestPosition(panel);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        assertTrue(duration < 1000);
    }

    @Test
    @DisplayName("测试：人类玩家冲四，AI 必须立刻拦截")
    void testHumanStraightFourDefense() {
        activePointsMap.clear();
        InitCellListPool();

        int[][] playerPos = {{3, 3}, {4, 4}, {5, 5}, {6, 6}};
        for (int[] pos : playerPos) {
            panel[pos[0]][pos[1]] = PLAYER_COLOR; SaveActiveChess(pos[0], pos[1]);
        }

        Point bestPos = getBestPosition(panel);
        boolean isInterceptionSuccessful = (bestPos.x == 2 && bestPos.y == 2) ||
                (bestPos.x == 7 && bestPos.y == 7);

        assertTrue(isInterceptionSuccessful);
    }

    @Test
    @DisplayName("测试：严谨性 - 无论局面如何，AI 决不能在已有棋子的位置落子")
    void testNoDuplicateMove() {
        InitCellListPool();
        activePointsMap.clear();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                panel[i][j] = PLAYER_COLOR; SaveActiveChess(i, j);
            }
        }

        panel[0][0] = 0; RemoveActiveChess(0, 0);
        panel[14][14] = 0; RemoveActiveChess(14, 14);

        Point move1 = getBestPosition(panel);
        assertTrue(panel[move1.x][move1.y] == 0);

        panel[move1.x][move1.y] = AI_COLOR; SaveActiveChess(move1.x, move1.y);

        Point move2 = getBestPosition(panel);
        assertNotNull(move2);
        assertNotEquals(move1.x * size + move1.y, move2.x * size + move2.y);
        assertEquals(0, panel[move2.x][move2.y]);
    }

    @Test
    @DisplayName("综合性能测试 - 不同阶段棋盘")
    void testComprehensivePerformance() {
        InitCellListPool();
        activePointsMap.clear();
        MinimaxCnt = 0;

        String[] scenarios = {"开局", "中盘", "终局", "进攻危机", "防守危机", "双活三"};
        for (int scenario = 0; scenario < scenarios.length; scenario++) {
            clearBoard();
            setupScenario(scenario);

            Point best = getBestPosition(panel);
            assertNotNull(best);
            assertTrue(panel[best.x][best.y] == 0);
        }
    }

    public int quickSniff(byte[][] panel, int x, int y) {
        int maxSeq = 0;
        for (int d = 0; d < 4; d++) {
            int dx = Direction[d][0], dy = Direction[d][1];
            int ai = 1, hu = 1;

            for (int j = 1; j <= 3; j++) {
                int tx = x + dx * j, ty = y + dy * j;
                if (!BoardUtils.IsLegal(tx, ty)) break;
                if (panel[tx][ty] == AI_COLOR) ai++;
                else if (panel[tx][ty] == PLAYER_COLOR) hu++;
                else break;
            }
            for (int j = 1; j <= 3; j++) {
                int tx = x - dx * j, ty = y - dy * j;
                if (!BoardUtils.IsLegal(tx, ty)) break;
                if (panel[tx][ty] == AI_COLOR) ai++;
                else if (panel[tx][ty] == PLAYER_COLOR) hu++;
                else break;
            }
            maxSeq = Math.max(maxSeq, Math.max(ai, hu));
            if (maxSeq >= 5) return 5;
        }
        return maxSeq;
    }

    public void clearBoard() {
        for (int i = 0; i < size; i++) {
            Arrays.fill(panel[i], (byte)0);
        }
        Arrays.fill(CellsCnt, 0);
    }

    public void setupScenario(int scenario) {
        if (scenario == 5) {
            panel[7][5] = AI_COLOR; SaveActiveChess(7, 5);
            panel[7][6] = AI_COLOR; SaveActiveChess(7, 6);
            panel[6][7] = AI_COLOR; SaveActiveChess(6, 7);
            panel[5][7] = AI_COLOR; SaveActiveChess(5, 7);
        } else {
            panel[7][7] = PLAYER_COLOR; SaveActiveChess(7, 7);
        }
    }

}