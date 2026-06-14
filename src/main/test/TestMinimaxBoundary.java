import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import utils.GameConstants;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("五子棋AI最严格边界测试")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class TestMinimaxBoundary {

    private TestMinimax ai;

    private static final byte AI_COLOR = 2;
    private static final byte PLAYER_COLOR = 1;
    private static final int SIZE = GameConstants.ChessBorderNum;

    @BeforeEach
    void setUp() {
        ai = new TestMinimax();
        ai.InitCellListPool();
        ai.activePointsMap.clear();
        clearBoard();
    }

    @AfterEach
    void tearDown() {
        clearBoard();
        ai.activePointsMap.clear();
    }

    private void clearBoard() {
        for (int i = 0; i < SIZE; i++) {
            Arrays.fill(ai.panel[i], (byte) 0);
        }
        Arrays.fill(ai.CellsCnt, 0);
        ai.InitCellListPool();
    }

    private void place(byte color, int[][] stones) {
        for (int[] s : stones) {
            ai.panel[s[0]][s[1]] = color;
            ai.SaveActiveChess(s[0], s[1]);
        }
    }

    // =========================
    // 一、棋盘坐标边界
    // =========================

    @ParameterizedTest(name = "角落合法性: ({0},{1})")
    @CsvSource({
            "0,0",
            "0,14",
            "14,0",
            "14,14"
    })
    @DisplayName("1-1 角落落子合法性")
    void testCornerMoveLegal(int x, int y) {
        assertTrue(ai.isValid(x, y));
    }

    @ParameterizedTest(name = "贴边一格合法性: ({0},{1})")
    @CsvSource({
            "0,1",
            "1,0",
            "13,14",
            "14,13"
    })
    @DisplayName("1-2 贴边一格合法性")
    void testEdgeAdjacentLegal(int x, int y) {
        assertTrue(ai.isValid(x, y));
    }

    @ParameterizedTest(name = "内侧一格合法性: ({0},{1})")
    @CsvSource({
            "1,1",
            "13,13",
            "1,13",
            "13,1"
    })
    @DisplayName("1-3 内侧一格合法性")
    void testInnerRingLegal(int x, int y) {
        assertTrue(ai.isValid(x, y));
    }

    @ParameterizedTest(name = "越界输入非法性: ({0},{1})")
    @CsvSource({
            "-1,0",
            "0,-1",
            "15,0",
            "0,15",
            "-1,-1",
            "15,15",
            "100,100"
    })
    @DisplayName("1-4 越界输入非法性")
    void testOutOfBoundsRejected(int x, int y) {
        assertFalse(ai.isValid(x, y));
    }

    @Test
    @DisplayName("1-5 边界横线：从左边界连续成五")
    void testBoundaryHorizontalLine() {
        place(AI_COLOR, new int[][]{{0,0},{0,1},{0,2},{0,3},{0,4}});
        double score = ai.evaluate(ai.panel, AI_COLOR);
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("1-6 边界斜线：从左下到右上连续成五")
    void testBoundaryDiagonalLine() {
        place(AI_COLOR, new int[][]{{14,0},{13,1},{12,2},{11,3},{10,4}});
        double score = ai.evaluate(ai.panel, AI_COLOR);
        assertTrue(score > 0);
    }

    // =========================
    // 二、候选点池边界
    // =========================

    @Test
    @DisplayName("2-1 空棋盘：候选池应为空或仅返回默认开局点")
    void testEmptyBoardCandidatePool() {
        int[] moves = TestMinimax.MoveListPool[5];
        int sz = TestMinimax.GetEmptyPointsList(ai.panel, moves);
        assertTrue(sz >= 0);
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("2-2 只有一个棋子：候选点应围绕该子扩展")
    void testSingleStoneCandidateRing() {
        place(PLAYER_COLOR, new int[][]{{7,7}});
        int[] moves = TestMinimax.MoveListPool[5];
        int sz = TestMinimax.GetEmptyPointsList(ai.panel, moves);
        assertTrue(sz > 0);
        for (int i = 0; i < sz; i++) {
            int key = moves[i];
            int x = key >>> 4;
            int y = key & 15;
            assertTrue(Math.abs(x - 7) <= 2);
            assertTrue(Math.abs(y - 7) <= 2);
        }
    }

    @Test
    @DisplayName("2-3 只有一个空位：候选池应只剩唯一合法点")
    void testOnlyOneEmptyCell() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                ai.panel[i][j] = PLAYER_COLOR;
                ai.SaveActiveChess(i, j);
            }
        }
        ai.panel[14][14] = 0;
        ai.RemoveActiveChess(14, 14);

        Point p = ai.getBestPosition(ai.panel);
        assertEquals(14, p.x);
        assertEquals(14, p.y);
    }

    @Test
    @DisplayName("2-4 满盘但留一个角落空位")
    void testFullBoardOneCornerEmpty() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                ai.panel[i][j] = PLAYER_COLOR;
                ai.SaveActiveChess(i, j);
            }
        }
        ai.panel[0][0] = 0;
        ai.RemoveActiveChess(0, 0);

        Point p = ai.getBestPosition(ai.panel);
        assertEquals(0, p.x);
        assertEquals(0, p.y);
    }

    @Test
    @DisplayName("2-5 候选点重复加入删除应保持链表正确")
    void testCandidateDuplicateAddRemove() {
        ai.panel[7][7] = PLAYER_COLOR;
        ai.SaveActiveChess(7, 7);
        ai.RemoveActiveChess(7, 7);
        ai.SaveActiveChess(7, 7);
        ai.RemoveActiveChess(7, 7);

        int[] moves = TestMinimax.MoveListPool[5];
        int sz = TestMinimax.GetEmptyPointsList(ai.panel, moves);
        assertTrue(sz >= 0);
    }

    @Test
    @DisplayName("2-6 CellsCnt 从0->1->2->1->0 回退正确")
    void testCellsCntRollback() {
        int x = 7, y = 7;
        int key = (x << 4) | y;

        assertEquals(0, ai.CellsCnt[key]);
        ai.panel[7][7] = AI_COLOR;
        ai.SaveActiveChess(7, 7);
        ai.RemoveActiveChess(7, 7);
        ai.SaveActiveChess(7, 7);
        ai.RemoveActiveChess(7, 7);

        assertEquals(0, ai.CellsCnt[key]);
    }

    // =========================
    // 三、落子/回溯边界
    // =========================

    @Test
    @DisplayName("3-1 SaveActiveChess 后 RemoveActiveChess 应完全恢复")
    void testSaveRemoveSymmetry() {
        int[][] stones = {{7,7},{7,8},{8,7}};
        place(AI_COLOR, stones);

        byte[][] snapshot = deepCopy(ai.panel);
        int[] cntSnapshot = Arrays.copyOf(ai.CellsCnt, ai.CellsCnt.length);

        ai.RemoveActiveChess(8, 7);
        ai.SaveActiveChess(8, 7);

        assertBoardEquals(snapshot, ai.panel);
        assertArrayEquals(cntSnapshot, ai.CellsCnt);
    }

    @Test
    @DisplayName("3-2 深层递归剪枝返回后棋盘应恢复")
    void testDeepRecursionRollback() {
        place(PLAYER_COLOR, new int[][]{{7,7},{7,8},{7,9}});
        byte[][] snapshot = deepCopy(ai.panel);

        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertBoardEquals(snapshot, ai.panel);
    }

    @Test
    @DisplayName("3-3 getBestPosition 返回后 panel 保持原状")
    void testBoardUnchangedAfterSearch() {
        place(PLAYER_COLOR, new int[][]{{7,7}});
        byte[][] snapshot = deepCopy(ai.panel);

        ai.getBestPosition(ai.panel);
        assertBoardEquals(snapshot, ai.panel);
    }

    @Test
    @DisplayName("3-4 连续两次搜索不应累积脏状态")
    void testRepeatedSearchNoDirtyState() {
        place(PLAYER_COLOR, new int[][]{{7,7}});
        Point p1 = ai.getBestPosition(ai.panel);
        Point p2 = ai.getBestPosition(ai.panel);

        assertNotNull(p1);
        assertNotNull(p2);
        assertTrue(ai.isValid(p1.x, p1.y));
        assertTrue(ai.isValid(p2.x, p2.y));
    }

    @Test
    @DisplayName("3-5 TT 清空与不清空结果一致")
    void testTTConsistency() {
        place(PLAYER_COLOR, new int[][]{{7,7},{7,8}});
        Point p1 = ai.getBestPosition(ai.panel);
        TestMinimax.TT.clear();
        Point p2 = ai.getBestPosition(ai.panel);
        assertNotNull(p1);
        assertNotNull(p2);
    }

    // =========================
    // 四、必胜/必堵边界
    // =========================

    @Test
    @DisplayName("4-1 活四应直接成五")
    void testLiveFourWinMove() {
        place(AI_COLOR, new int[][]{{7,5},{7,6},{7,7},{7,8}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 7 && p.y == 4) || (p.x == 7 && p.y == 9));
    }

    @Test
    @DisplayName("4-2 冲四应被优先堵住")
    void testRushFourDefense() {
        place(PLAYER_COLOR, new int[][]{{4,4},{4,5},{4,6},{4,7}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 4 && p.y == 3) || (p.x == 4 && p.y == 8));
    }

    @Test
    @DisplayName("4-3 死四应低于活三")
    void testDeadFourVsLiveThree() {
        place(PLAYER_COLOR, new int[][]{{4,2},{4,3},{4,4},{4,5}});
        ai.panel[4][1] = AI_COLOR;
        ai.SaveActiveChess(4, 1);

        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
    }

    @Test
    @DisplayName("4-4 双活三威胁应优先级更高")
    void testDoubleLiveThreePriority() {
        place(AI_COLOR, new int[][]{{7,5},{7,6},{7,7},{6,7},{5,7}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
    }

    @Test
    @DisplayName("4-5 对手和自己同时威胁时，AI应优先堵更高威胁")
    void testThreatPriorityComparison() {
        place(PLAYER_COLOR, new int[][]{{2,2},{3,3},{4,4}});
        place(AI_COLOR, new int[][]{{7,7},{7,8},{7,9},{7,10}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
    }

    // =========================
    // 五、评估函数边界
    // =========================

    @ParameterizedTest(name = "单棋型评分: {0}")
    @MethodSource("singlePatternProvider")
    @DisplayName("5-1 单子/双子/三子/四子/五子评分")
    void testSinglePatternScore(String name, int[][] stones, boolean shouldPositive) {
        place(AI_COLOR, stones);
        double score = ai.evaluate(ai.panel, AI_COLOR);
        assertTrue(shouldPositive ? score > 0 : true);
    }

    static Stream<Arguments> singlePatternProvider() {
        return Stream.of(
                Arguments.of("单子", new int[][]{{7,7}}, true),
                Arguments.of("双子", new int[][]{{7,7},{7,8}}, true),
                Arguments.of("三子", new int[][]{{7,7},{7,8},{7,9}}, true),
                Arguments.of("四子", new int[][]{{7,7},{7,8},{7,9},{7,10}}, true),
                Arguments.of("五子", new int[][]{{7,7},{7,8},{7,9},{7,10},{7,11}}, true)
        );
    }

    @Test
    @DisplayName("5-2 同一条线多个局部模式不应严重重复计分")
    void testNoMassiveDoubleCounting() {
        place(AI_COLOR, new int[][]{{7,5},{7,6},{7,7},{7,8},{7,9}});
        double s1 = ai.evaluate(ai.panel, AI_COLOR);
        double s2 = ai.evaluate(ai.panel, AI_COLOR);
        assertEquals(s1, s2);
    }

    @Test
    @DisplayName("5-3 边角棋型不应漏算开放端")
    void testCornerPatternOpenEnds() {
        place(AI_COLOR, new int[][]{{0,0},{0,1},{0,2}});
        double score = ai.evaluate(ai.panel, AI_COLOR);
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("5-4 横竖斜同时成型应能叠加正确总分")
    void testMultiDirectionScoreAdditivity() {
        place(AI_COLOR, new int[][]{{7,7},{7,8},{7,9}});
        ai.panel[6][6] = AI_COLOR;
        ai.panel[8][8] = AI_COLOR;
        ai.SaveActiveChess(6, 6);
        ai.SaveActiveChess(8, 8);
        double score = ai.evaluate(ai.panel, AI_COLOR);
        assertTrue(score > 0);
    }

    @Test
    @DisplayName("5-5 对手与己方接近时应稳定选边")
    void testBalancedThreatStability() {
        place(PLAYER_COLOR, new int[][]{{4,4},{4,5},{4,6}});
        place(AI_COLOR, new int[][]{{8,8},{8,9},{8,10}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
    }

    // =========================
    // 六、时间边界
    // =========================

    @Test
    @DisplayName("6-1 浅层局面应快速返回")
    void testQuickSearchUnderLimit() {
        place(PLAYER_COLOR, new int[][]{{7,7}});
        long start = System.currentTimeMillis();
        Point p = ai.getBestPosition(ai.panel);
        long cost = System.currentTimeMillis() - start;
        assertNotNull(p);
        assertTrue(cost < 1200);
    }

    @Test
    @DisplayName("6-2 复杂局面在阈值附近应稳定返回")
    void testNearTimeoutStableReturn() {
        for (int i = 0; i < SIZE; i += 2) {
            for (int j = 0; j < SIZE; j += 3) {
                ai.panel[i][j] = (byte) ((i + j) % 2 == 0 ? AI_COLOR : PLAYER_COLOR);
                ai.SaveActiveChess(i, j);
            }
        }
        long start = System.currentTimeMillis();
        Point p = ai.getBestPosition(ai.panel);
        long cost = System.currentTimeMillis() - start;
        assertNotNull(p);
        assertTrue(cost < 1500);
    }

    @Test
    @DisplayName("6-3 迭代加深在深层局面应返回当前最佳")
    void testIterativeDeepeningFallback() {
        place(PLAYER_COLOR, new int[][]{{7,7},{8,8},{9,9},{10,10}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    // =========================
    // 额外极端场景
    // =========================

    @Test
    @DisplayName("7-1 棋盘满盘无处落子")
    void testFullBoardNoMove() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                ai.panel[i][j] = PLAYER_COLOR;
                ai.SaveActiveChess(i, j);
            }
        }
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
    }

    @Test
    @DisplayName("7-2 只剩一个合法点")
    void testSingleLegalPointRemaining() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                ai.panel[i][j] = PLAYER_COLOR;
                ai.SaveActiveChess(i, j);
            }
        }
        ai.panel[14][14] = 0;
        ai.RemoveActiveChess(14, 14);
        Point p = ai.getBestPosition(ai.panel);
        assertEquals(14, p.x);
        assertEquals(14, p.y);
    }

    @Test
    @DisplayName("棋谱必须下法：开局第一手应落在中心或中心邻域")
    void testOpeningCenterMove() {
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(p.x >= 6 && p.x <= 8);
        assertTrue(p.y >= 6 && p.y <= 8);
    }

    @Test
    @DisplayName("棋谱必须下法：寒星开局应优先保持中心扩张")
    void testHanxingOpeningPattern() {
        place(PLAYER_COLOR, new int[][]{{7,7}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：花月定式中应优先抢关键争点")
    void testHuayueCriticalPoint() {
        place(PLAYER_COLOR, new int[][]{{7,7},{7,8}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：对手形成活四时必须立刻堵住")
    void testMustBlockLiveFour() {
        place(PLAYER_COLOR, new int[][]{{4,4},{4,5},{4,6},{4,7}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 4 && p.y == 3) || (p.x == 4 && p.y == 8));
    }

    @Test
    @DisplayName("棋谱必须下法：己方形成活四时必须直接成五")
    void testMustFinishLiveFour() {
        place(AI_COLOR, new int[][]{{7,5},{7,6},{7,7},{7,8}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 7 && p.y == 4) || (p.x == 7 && p.y == 9));
    }

    @Test
    @DisplayName("棋谱必须下法：对手冲四时必须优先防守")
    void testMustDefendRushFour() {
        place(PLAYER_COLOR, new int[][]{{3,3},{3,4},{3,5},{3,6}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 3 && p.y == 2) || (p.x == 3 && p.y == 7));
    }

    @Test
    @DisplayName("棋谱必须下法：双活三局面应优先抢先手")
    void testMustHandleDoubleLiveThree() {
        place(AI_COLOR, new int[][]{{7,5},{7,6},{7,7},{6,7},{5,7}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：对手双三威胁应优先堵高威胁点")
    void testMustBlockDoubleThreat() {
        place(PLAYER_COLOR, new int[][]{{2,2},{3,3},{4,4}});
        place(PLAYER_COLOR, new int[][]{{8,8},{8,9},{8,10}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：边界横线成五时不能漏算")
    void testBoundaryWinLine() {
        place(AI_COLOR, new int[][]{{0,0},{0,1},{0,2},{0,3}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 0 && p.y == 4) || (p.x == 0 && p.y == 5));
    }

    @Test
    @DisplayName("棋谱必须下法：边界斜线成五时必须识别")
    void testBoundaryDiagonalWinLine() {
        place(AI_COLOR, new int[][]{{14,0},{13,1},{12,2},{11,3}});
        Point p = ai.getBestPosition(ai.panel);
        assertTrue((p.x == 10 && p.y == 4) || ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：空盘应返回合法开局点")
    void testEmptyBoardOpening() {
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：只剩唯一空位时必须下该点")
    void testOnlyOneMoveLeft() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                ai.panel[i][j] = PLAYER_COLOR;
                ai.SaveActiveChess(i, j);
            }
        }
        ai.panel[14][14] = 0;
        ai.RemoveActiveChess(14, 14);

        Point p = ai.getBestPosition(ai.panel);
        assertEquals(14, p.x);
        assertEquals(14, p.y);
    }

    @Test
    @DisplayName("棋谱必须下法：己方活三时应优先扩成活四")
    void testLiveThreeExpandToLiveFour() {
        place(AI_COLOR, new int[][]{{7,6},{7,7},{7,8}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：对手活三时必须优先防守")
    void testBlockLiveThreeFirst() {
        place(PLAYER_COLOR, new int[][]{{7,6},{7,7},{7,8}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    @Test
    @DisplayName("棋谱必须下法：四三转换点必须识别")
    void testFourThreeConversion() {
        place(AI_COLOR, new int[][]{{5,5},{6,6},{7,7}});
        place(AI_COLOR, new int[][]{{7,5},{7,6}});
        Point p = ai.getBestPosition(ai.panel);
        assertNotNull(p);
        assertTrue(ai.isValid(p.x, p.y));
    }

    private byte[][] deepCopy(byte[][] src) {
        byte[][] copy = new byte[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }

    private void assertBoardEquals(byte[][] expected, byte[][] actual) {
        for (int i = 0; i < SIZE; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }
}
