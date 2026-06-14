import ai.AI_Engine;
import ai.Minimax.MiniMaxEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import utils.GameConstants;

import java.awt.Point;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MiniMaxEngine 测试类
 * 
 * 测试覆盖：
 * 1. 引擎初始化和基本功能
 * 2. 不同深度设置
 * 3. 不同时间限制
 * 4. 先手/后手测试
 * 5. 边界情况测试
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class MiniMaxEngineTest {

    private static final int BOARD_SIZE = GameConstants.ChessBorderNum;

    /**
     * 初始化空棋盘
     */
    private byte[][] createEmptyBoard() {
        return new byte[BOARD_SIZE][BOARD_SIZE];
    }

    /**
     * 在指定位置放置棋子
     */
    private byte[][] placePieces(byte[][] board, int... positions) {
        byte color = 1;
        for (int i = 0; i < positions.length; i += 2) {
            int x = positions[i];
            int y = positions[i + 1];
            if (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE) {
                board[x][y] = color;
                color = (byte) (3 - color);
            }
        }
        return board;
    }

    // ==================== 高深度测试 ====================

    @Test
    @DisplayName("1.5 高深度测试 - 10层")
    void testDepth10() {
        AI_Engine engine = new MiniMaxEngine(9, 2000);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("1.6 高深度测试 - 12层")
    void testDepth12() {
        AI_Engine engine = new MiniMaxEngine( 12, 3000);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("1.7 高深度防守测试 - 10层四连防守")
    void testDepth10Defense() {
        AI_Engine engine = new MiniMaxEngine( 10, 2000);
        byte[][] board = createEmptyBoard();
        // 白棋已有四连
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 必须堵住
        assertTrue((move.x == 7 && move.y == 6) || (move.x == 7 && move.y == 11),
                "预期堵住四连，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("1.8 高深度进攻测试 - 10层三连进攻")
    void testDepth10Attack() {
        AI_Engine engine = new MiniMaxEngine( 10, 2000);
        byte[][] board = createEmptyBoard();
        // 黑棋已有三连
        board[7][7] = 1;
        board[7][8] = 1;
        board[7][9] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 应该进攻
        assertTrue((move.x == 7 && (move.y == 6 || move.y == 10)),
                "预期进攻，实际选择: (" + move.x + "," + move.y + ")");
    }

    // ==================== 基础测试 ====================

    @Test
    @DisplayName("2.1 创建引擎 - 默认参数")
    void testCreateEngine_DefaultParams() {
        AI_Engine engine = new MiniMaxEngine();
        
        assertNotNull(engine);
        assertTrue(engine instanceof MiniMaxEngine);
    }

    @Test
    @DisplayName("2.2 创建引擎 - 自定义深度和时间限制")
    void testCreateEngine_CustomParams() {
        int customDepth = 8;
        long customTime = 1000;
        
        MiniMaxEngine engine = new MiniMaxEngine( customDepth, customTime);
        
        assertNotNull(engine);
        assertTrue(engine instanceof AI_Engine);
    }

    @Test
    @DisplayName("2.3 黑棋先手 - 第一手下法")
    void testBlackFirstMove() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE, "X坐标超出范围: " + move.x);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE, "Y坐标超出范围: " + move.y);
    }

    @Test
    @DisplayName("2.4 白棋后手 - 第一手下法")
    void testWhiteFirstMove() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 不同深度测试 ====================

    @DisplayName("2.x 不同搜索深度测试")
    void testSearchDepth(int depth) {
        AI_Engine engine = new MiniMaxEngine( depth, 1000);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 不同时间限制测试 ====================

    @DisplayName("3.x 不同时间限制测试")
    void testTimeLimit(long timeLimit) {
        AI_Engine engine = new MiniMaxEngine( 6, timeLimit);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 棋盘状态测试 ====================

    @Test
    @DisplayName("4.1 空棋盘 - 返回有效位置")
    void testEmptyBoard() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(0, board[move.x][move.y]); // 确保选择空位
    }

    @Test
    @DisplayName("4.2 中心已有棋子 - 避开中心")
    void testCenterOccupied() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        board[7][7] = 2; // 白棋先占中心
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertNotEquals(7, move.x);
        assertNotEquals(7, move.y);
    }

    @Test
    @DisplayName("4.3 简单局面 - 能找到进攻位置")
    void testSimpleAttack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋已有三连
        board[7][7] = 1;
        board[7][8] = 1;
        board[7][9] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 应该扩展四连
        assertTrue((move.x == 7 && move.y == 6) || (move.x == 7 && move.y == 10),
                "预期扩展四连，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("4.4 防守局面 - 能阻止对手获胜")
    void testDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋已有四连，黑棋必须堵
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 必须堵两端之一
        assertTrue((move.x == 7 && move.y == 6) || (move.x == 7 && move.y == 11),
                "预期堵住四连，实际选择: (" + move.x + "," + move.y + ")");
    }

    // ==================== 四连防守测试 ====================

    @Test
    @DisplayName("5.1 四连防守 - 左端开放必须防守左端")
    void testFourInLine_LeftOpen() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋四连，左端开放，右端有黑棋挡
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        board[7][11] = 1; // 右端已堵
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertEquals(6, move.y); // 必须堵左端
    }

    @Test
    @DisplayName("5.2 四连防守 - 右端开放必须防守右端")
    void testFourInLine_RightOpen() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋四连，右端开放，左端有黑棋挡
        board[7][6] = 1; // 左端已堵
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertEquals(11, move.y); // 必须堵右端
    }

    @Test
    @DisplayName("5.3 四连防守 - 两端都开放")
    void testFourInLine_BothOpen() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋四连，两端都开放
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertTrue(move.y == 6 || move.y == 11, "应该堵住一端，实际选择: (" + move.x + "," + move.y + ")");
    }

    // ==================== 三连防守测试 ====================
    @Test
    @DisplayName("6.2 三连进攻 - 能形成四连")
    void testThreeInLine_Attack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋三连，两端开放
        board[7][7] = 1;
        board[7][8] = 1;
        board[7][9] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertTrue(move.y == 6 || move.y == 10, "应该进攻，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("6.3 三连防守 - 横向三连")
    void testThreeInLine_Horizontal() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋横向三连
        board[6][7] = 2;
        board[7][7] = 2;
        board[8][7] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue((move.x == 5 && move.y == 7) || (move.x == 9 && move.y == 7),
                "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("6.4 三连防守 - 纵向三连")
    void testThreeInLine_Vertical() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋纵向三连
        board[7][6] = 2;
        board[7][7] = 2;
        board[7][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertTrue(move.y == 5 || move.y == 9, "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    // ==================== 棋谱经典场景测试 ====================

    @Test
    @DisplayName("8.1 棋谱场景 - 攻防转换")
    void testClassicPattern_AttackDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 双方都有威胁的局面
        board[7][7] = 1;
        board[7][8] = 1;
        board[7][9] = 1;
        board[8][7] = 2;
        board[8][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("8.2 棋谱场景 - 双三威胁")
    void testClassicPattern_DoubleThree() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 制造双三局面
        board[7][7] = 1;
        board[7][8] = 1;
        board[8][7] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("8.3 棋谱场景 - 长连防守")
    void testClassicPattern_LongChain() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋五连快成了
        board[7][5] = 2;
        board[7][6] = 2;
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertEquals(10, move.y); // 必须挡住最后位置
    }

    @Test
    @DisplayName("8.4 棋谱场景 - 斜向攻防")
    void testClassicPattern_Diagonal() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 斜向三连
        board[5][5] = 1;
        board[6][6] = 1;
        board[7][7] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 多回合测试 ====================

    @Test
    @DisplayName("9.1 多回合测试 - 连续下棋")
    void testMultipleMoves() {
        MiniMaxEngine blackEngine = new MiniMaxEngine();
        MiniMaxEngine whiteEngine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 进行多回合
        for (int i = 0; i < 5; i++) {
            // 黑棋
            Point blackMove = blackEngine.findBestMove(board);
            assertNotNull(blackMove);
            assertTrue(board[blackMove.x][blackMove.y] == 0);
            board[blackMove.x][blackMove.y] = 1;
            
            // 白棋
            Point whiteMove = whiteEngine.findBestMove(board);
            assertNotNull(whiteMove);
            assertTrue(board[whiteMove.x][whiteMove.y] == 0);
            board[whiteMove.x][whiteMove.y] = 2;
        }
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("10.1 角落位置测试")
    void testCornerPosition() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 角落已有棋子
        board[0][0] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("10.2 边缘位置测试")
    void testEdgePosition() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 边缘已有棋子
        board[0][7] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("10.3 最小深度(2层)测试")
    void testMinimumDepth() {
        AI_Engine engine = new MiniMaxEngine( 2, 500);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("10.4 最小时间限制测试")
    void testMinimumTimeLimit() {
        AI_Engine engine = new MiniMaxEngine( 9, 200000);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 对称测试 ====================

    @Test
    @DisplayName("11.1 左右对称局面")
    void testHorizontalSymmetry() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 对称局面
        board[7][5] = 1;
        board[7][9] = 1;
        board[7][6] = 2;
        board[7][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("11.2 垂直对称局面")
    void testVerticalSymmetry() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 对称局面
        board[5][7] = 1;
        board[9][7] = 1;
        board[6][7] = 2;
        board[8][7] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 性能测试 ====================

    @Test
    @DisplayName("12.1 响应时间测试")
    void testResponseTime() {
        AI_Engine engine = new MiniMaxEngine( 4, 5000);
        byte[][] board = createEmptyBoard();
        
        long startTime = System.currentTimeMillis();
        Point move = engine.findBestMove(board);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(move);
        long responseTime = endTime - startTime;
        assertTrue(responseTime <= 600, "响应时间过长: " + responseTime + "ms");
    }

    // ==================== 引擎复用测试 ====================

    @Test
    @DisplayName("13.1 引擎复用 - 多局游戏")
    void testEngineReuse() {
        AI_Engine engine = new MiniMaxEngine();
        
        // 第一局
        byte[][] board1 = createEmptyBoard();
        Point move1 = engine.findBestMove(board1);
        assertNotNull(move1);
        
        // 第二局（不同局面）
        byte[][] board2 = createEmptyBoard();
        board2[7][7] = 2; // 白棋先行
        Point move2 = engine.findBestMove(board2);
        assertNotNull(move2);
        
        // 第三局（空棋盘）
        byte[][] board3 = createEmptyBoard();
        Point move3 = engine.findBestMove(board3);
        assertNotNull(move3);
    }

    // ==================== 接口实现验证 ====================

    @Test
    @DisplayName("14.1 实现AI_Engine接口")
    void testImplementsInterface() {
        AI_Engine engine = new MiniMaxEngine();
        
        assertTrue(AI_Engine.class.isAssignableFrom(engine.getClass()));
    }

    @Test
    @DisplayName("14.2 findBestMove方法存在")
    void testFindBestMoveExists() {
        AI_Engine engine = new MiniMaxEngine();
        
        assertNotNull(engine);
        // 通过调用验证方法存在
        Point move = engine.findBestMove(createEmptyBoard());
        assertNotNull(move);
    }

    // ==================== 额外防守场景测试 ====================

    @Test
    @DisplayName("15.1 斜向四连防守")
    void testDiagonalFourDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋斜向四连
        board[5][5] = 2;
        board[6][6] = 2;
        board[7][7] = 2;
        board[8][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 应该防守斜向四连
        assertTrue((move.x == 4 && move.y == 4) || (move.x == 9 && move.y == 9),
                "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("15.2 反斜向四连防守")
    void testAntiDiagonalFourDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋反斜向四连
        board[5][9] = 2;
        board[6][8] = 2;
        board[7][7] = 2;
        board[8][6] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        // 应该防守
        assertTrue((move.x == 4 && move.y == 10) || (move.x == 9 && move.y == 5),
                "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("15.3 多方向威胁")
    void testMultiDirectionThreat() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋两个方向都有三连
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[8][7] = 2;
        board[9][7] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("15.4 一字长蛇防守")
    void testLongSnakeDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋六连
        board[7][5] = 2;
        board[7][6] = 2;
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[7][10] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertTrue(move.y == 4 || move.y == 11,
                "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("15.5 双四威胁")
    void testDoubleFourThreat() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 白棋形成双四威胁
        board[7][7] = 2;
        board[7][8] = 2;
        board[7][9] = 2;
        board[8][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    // ==================== 额外进攻场景测试 ====================

    @Test
    @DisplayName("16.1 二连进攻")
    void testTwoInLine_Attack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋二连
        board[7][7] = 1;
        board[7][8] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.x);
        assertTrue(move.y == 6 || move.y == 9,
                "应该进攻，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("16.2 纵向三连进攻")
    void testVerticalThree_Attack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋纵向三连
        board[5][7] = 1;
        board[6][7] = 1;
        board[7][7] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(7, move.y);
        assertTrue(move.x == 4 || move.x == 8,
                "应该进攻，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("16.3 斜向三连进攻")
    void testDiagonalThree_Attack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋斜向三连
        board[5][5] = 1;
        board[6][6] = 1;
        board[7][7] = 1;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("16.5 跳三进攻")
    void testJumpThree_Attack() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 黑棋跳三
        board[7][7] = 1;
        board[7][9] = 1;
        
        Point move = engine.findBestMove(board);
        System.out.println(move);
        assertNotNull(move);
        assertEquals(7, move.x);
        assertEquals(8, move.y); // 应该在中间
    }

    // ==================== 边界情况深入测试 ====================

    @Test
    @DisplayName("17.1 棋盘边缘四连防守")
    void testEdgeFourDefense() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 棋盘边缘四连
        board[0][5] = 2;
        board[0][6] = 2;
        board[0][7] = 2;
        board[0][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertEquals(0, move.x);
        assertTrue(move.y == 4 || move.y == 9,
                "应该防守，实际选择: (" + move.x + "," + move.y + ")");
    }

    @Test
    @DisplayName("17.2 复杂局面攻防")
    void testComplexSituation() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        // 复杂局面
        board[7][7] = 1;
        board[7][8] = 1;
        board[7][9] = 1;
        board[8][7] = 2;
        board[8][8] = 2;
        board[9][7] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("17.3 棋盘接近填满")
    void testAlmostFullBoard() {
        AI_Engine engine = new MiniMaxEngine();
        byte[][] board = createEmptyBoard();
        
        // 填满大部分棋盘
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = (byte) ((i + j) % 2 + 1);
            }
        }
        board[7][7] = 0; // 留一个空位
        
        Point move = engine.findBestMove(board);
        System.out.println(move);
        assertNotNull(move);
        assertEquals(7, move.x);
        assertEquals(7, move.y);
    }

    @Test
    @DisplayName("17.4 高深度复杂局面")
    void testDeepSearchComplex() {
        AI_Engine engine = new MiniMaxEngine( 10, 2000);
        byte[][] board = createEmptyBoard();
        
        // 放置多个棋子形成复杂局面
        board[7][5] = 1;
        board[7][6] = 1;
        board[7][8] = 2;
        board[7][9] = 2;
        board[8][6] = 1;
        board[8][8] = 2;
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }

    @Test
    @DisplayName("17.5 极端时间限制")
    void testExtremeTimeLimit() {
        AI_Engine engine = new MiniMaxEngine( 6, 50);
        byte[][] board = createEmptyBoard();
        
        Point move = engine.findBestMove(board);
        
        assertNotNull(move);
        assertTrue(move.x >= 0 && move.x < BOARD_SIZE);
        assertTrue(move.y >= 0 && move.y < BOARD_SIZE);
    }
}
