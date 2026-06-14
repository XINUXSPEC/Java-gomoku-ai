/*
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.GameConstants;

import static org.junit.jupiter.api.Assertions.*;

public class TestCellsListPool {
    class PointNode{
        public int Key;       // 坐标（x * border + y）
        public int NextIdx;   // 下一个空位的数组下标
        public int PrevIdx;   // 上一个空位的数组下标
    }
    static private final int border = GameConstants.ChessBorderNum; // 15
    static private int size = GameConstants.ChessBorderNum;
    static int totalCells = size * size;
    static TestMinimax.PointNode[] CellsListPool = new TestMinimax.PointNode[totalCells+2];
    static final int HEAD = totalCells,TAIL = totalCells+1;
    private final static int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};


    */
/**
     * @BeforeEach 代表在执行每一个 @Test 方法前，都会自动跑一次这个初始化。
     * 确保每个测试用例拿到的都是一个全新的、干净的满棋盘链表。
     *//*

    @BeforeEach
    @DisplayName("每轮测试前：无内存碎片初始化静态链表")
    //初始化
    void InitCellListPool(){
        // 先把全量数组对象 new 出来，避免 NullPointerException
        for (int i = 0; i < CellsListPool.length; i++) {
            CellsListPool[i] = new TestMinimax.PointNode();
            CellsListPool[i].Key = i;
        }
        CellsListPool[HEAD].NextIdx=0;CellsListPool[TAIL].NextIdx=-1;
        CellsListPool[HEAD].PrevIdx=-1;CellsListPool[TAIL].PrevIdx=totalCells-1;
        for (int i = 0; i < totalCells; i++) {
            CellsListPool[i].PrevIdx = i==0?HEAD:i-1;
            CellsListPool[i].NextIdx = i==totalCells-1?TAIL:i+1;
        }
    }

    //从静态链表中删除
    static public void DelOnCellsListPool(int x,int y){
        int key = x*border+y;
        if(CellsListPool[key].PrevIdx != -1 || CellsListPool[key].NextIdx != -1){
            CellsListPool[CellsListPool[key].PrevIdx].NextIdx = CellsListPool[key].NextIdx;
            CellsListPool[CellsListPool[key].NextIdx].PrevIdx = CellsListPool[key].PrevIdx;
            CellsListPool[key].PrevIdx=CellsListPool[key].NextIdx=-1;
        }
    }

    //加入静态链表中
    static public void AddOnCellsListPool(int x,int y){
        int key = x*border+y;
        if (CellsListPool[key].NextIdx == -1 && CellsListPool[key].PrevIdx == -1) {
            CellsListPool[key].PrevIdx = HEAD;
            CellsListPool[key].NextIdx = CellsListPool[HEAD].NextIdx;
            CellsListPool[CellsListPool[HEAD].NextIdx].PrevIdx = key;
            CellsListPool[HEAD].NextIdx = key;
        }
    }

    //将一个Cell附近的半径2以内的空格加加进来
    static public void AddOnCellsAround(byte[][] panel,int x,int y){
        for (int i = 0; i < 4; i++) {
            int dx=Direction[i][0],dy=Direction[i][1];
            for (int j = 1; j <= 2; j++) {
                int tx = x+j*dx,ty=y+j*dy,key=tx*border+ty;
                if (BoardUtils.IsLegal(tx,ty) && panel[tx][ty] == 0){
                    AddOnCellsListPool(tx,ty);
                }
                tx = x-j*dx;ty=y-j*dy;key=tx*border+ty;
                if (BoardUtils.IsLegal(tx,ty) && panel[tx][ty] == 0){
                    AddOnCellsListPool(tx,ty);
                }
            }
        }
    }

    //将一个Cell附近的半径2以内的空格删除进来
    static public void DelOnCellsAround(int x,int y){
        for (int i = 0; i < 4; i++) {
            int dx=Direction[i][0],dy=Direction[i][1];
            for (int j = 1; j <= 2; j++) {
                int tx = x+j*dx,ty=y+j*dy,key=tx*border+ty;
                if (BoardUtils.IsLegal(tx,ty)){
                    DelOnCellsListPool(tx,ty);
                }
                tx = x-j*dx;ty=y-j*dy;key=tx*border+ty;
                if (BoardUtils.IsLegal(tx,ty)){
                    DelOnCellsListPool(tx,ty);
                }
            }
        }
    }

    // --- 辅助测试工具方法：判断某个 key 还在不在空位链表中 ---
    private boolean isKeyInList(int targetKey) {
        int curr = CellsListPool[HEAD].NextIdx;
        while (curr != TAIL) {
            if (curr == targetKey) return true;
            curr = CellsListPool[curr].NextIdx;
        }
        return false;
    }

    // --- 核心调试打印工具：顺着 HEAD 往下摸，数数还剩多少空位，并把头部前几个高频变动的格子打印出来 ---
    static void PrintListState(String stepName) {
        System.out.println("\n========================================================");
        System.out.println("【当前步骤】: " + stepName);
        System.out.println("========================================================");

        int curr = CellsListPool[HEAD].NextIdx;
        int count = 0;

        System.out.print("链表头部追踪: HEAD -> ");
        while (curr != TAIL) {
            count++;
            // 为了防止 225 个点刷屏，我们只清晰打印出刚被头插法影响的、最前排的 10 个骨干节点
            if (count <= 10) {
                int tx = curr / border;
                int ty = curr % border;
                System.out.print(String.format("(%d,%d)[Key:%d] -> ", tx, ty, curr));
            }
            if (count == 11) {
                System.out.print("... -> ");
            }
            curr = CellsListPool[curr].NextIdx;
        }
        System.out.println("TAIL");
        System.out.println("当前静态链表中的【总剩余空位数】: " + count);
    }
    // =================================================================
    // JUnit 5 测试用例
    // =================================================================
    @Test
    @DisplayName("测试1：基础下子与强插头部复活验证")
    void testBasicAddAndRemove() {
        int x = 7, y = 7;
        int targetKey = x * border + y; // 112

        // 1. 下子摘除
        DelOnCellsListPool(x, y);
        assertFalse(isKeyInList(targetKey), "下子后112号必须在空位链表中隐形");

        // 2. 头插复活
        AddOnCellsListPool(x, y);
        assertTrue(isKeyInList(targetKey), "回溯后112号必须重新可以遍历");

        // 3. 核心断言：头插法会使其强行夺取大弟子之位
        assertEquals(targetKey, CellsListPool[HEAD].NextIdx, "新写法的复活节点必须强行占领HEAD正后方第一位");
    }

    @Test
    @DisplayName("测试2：连续双删后乱序单加，严证1号位被孤立且链表无断裂")
    void testRemoveTwoAndAddFirst() {
        // 1. 连续摘除 0 和 1（模拟落子）
        DelOnCellsListPool(0, 0);
        DelOnCellsListPool(0, 1);
        assertFalse(isKeyInList(0));
        assertFalse(isKeyInList(1));
        assertEquals(2, CellsListPool[HEAD].NextIdx, "连续双删0和1后，HEAD右手必须是2");

        // 2. 故意乱序回溯：只把第一个删掉的 0 捞出来强插头部
        AddOnCellsListPool(0, 0);

        // 3. 核心断言（你的直觉）：
        assertTrue(isKeyInList(0), "0 必须成功复活回到链表");
        assertFalse(isKeyInList(1), "1 还没正式调用Add，由于头插隔离，它此时绝对不能被意外带进链表！");

        // 4. 双向指针完美对称验证（自愈性检查）
        assertEquals(0, CellsListPool[HEAD].NextIdx, "HEAD 右手牵着新复活的 0");
        assertEquals(2, CellsListPool[0].NextIdx, "0 的右手跨过隐形的 1，直接拉住 2");
        assertEquals(0, CellsListPool[2].PrevIdx, "2 的左手也必须同步拉住 0，全盘完美契合，毫无断裂");
    }

    @Test
    @DisplayName("测试3：用Del手工制造空洞，验证AddOnCellsAround的8方向双向召唤")
    void testAddOnCellsAroundWithDel() {
        byte[][] panel = new byte[border][border];
        int centerX = 7, centerY = 7;

        DelOnCellsAround(centerX, centerY);
        PrintListState("1. 执行 DelOnCellsAround(7, 7) 之后");

        // -------------------------------------------------------------
        // 第二步：删除 (7,8) 周围（发生了重叠区二次删除）
        // -------------------------------------------------------------
        DelOnCellsAround(centerX, centerY + 1);
        PrintListState("2. 执行 DelOnCellsAround(7, 8) 之后");

        // -------------------------------------------------------------
        // 第三步：加回 (7,7) 周围（开始高频头插重组）
        // -------------------------------------------------------------
        AddOnCellsAround(panel, centerX, centerY);
        PrintListState("3. 执行 AddOnCellsAround(panel, 7, 7) 之后");

        // -------------------------------------------------------------
        // 第四步：加回 (7,8) 周围（交叉重叠区爆发二次头插）
        // -------------------------------------------------------------
        AddOnCellsAround(panel, centerX, centerY + 1);
        PrintListState("4. 执行 AddOnCellsAround(panel, 7, 8) 之后并完全自愈");

    }

    @Test
    @DisplayName("测试4：扫描时遇到已有落子，验证Del之后无法被强制唤醒")
    void testAddOnCellsAroundWithRealObstacle() {
        byte[][] panel = new byte[border][border];
        int centerX = 7, centerY = 7;

        // 模拟正方向 (8,7) 有一颗真实的棋子
        panel[8][7] = 1;

        // 模拟博弈树推进：用 Del 彻底隔离周围的 16 个待扫描点
        for (int i = 0; i < 4; i++) {
            int dx = Direction[i][0], dy = Direction[i][1];
            for (int j = 1; j <= 2; j++) {
                DelOnCellsListPool(centerX + j * dx, centerY + j * dy);
                DelOnCellsListPool(centerX - j * dx, centerY - j * dy);
            }
        }

        // 触发局部扫描加回
        AddOnCellsAround(panel, centerX, centerY);

        // 核心硬核断言：
        assertFalse(isKeyInList(8 * border + 7), "虽然触发了周围扫描，但(8,7)有棋子占着，绝不允许被加回空位链表");
        assertTrue(isKeyInList(6 * border + 7), "反方向的(6,7)是干净空格，必须畅通无阻地被召回");
    }

}
*/
