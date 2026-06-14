package ai.Minimax;

import   utils.GameConstants;

/**
 * 静态链表 - 用于高效管理棋盘空位列表
 * 
 * 特点：
 * 1. 使用数组模拟链表，避免动态内存分配
 * 2. O(1) 时间复杂度的插入/删除操作
 * 3. 适用于固定大小的棋盘场景
 */
public class StaticLinkedList {
    
    /** 节点类 */
    public static class PointNode {
        public int Key;
        public int NextIdx;
        public int PrevIdx;
    }

    // ==================== 常量定义 ====================
    public static final int BASE = 16;
    public static final int totalCells = BASE * BASE;
    public static final int HEAD = totalCells;
    public static final int TAIL = totalCells + 1;

    // ==================== 数据结构 ====================
    private final PointNode[] cellsList;    // 节点数组
    private final int[] cellsCnt;           // 邻近棋子计数器

    public StaticLinkedList() {
        cellsList = new PointNode[totalCells + 2];
        cellsCnt = new int[totalCells];
        
        for (int i = 0; i < cellsList.length; i++) {
            cellsList[i] = new PointNode();
            cellsList[i].Key = i;
            cellsList[i].NextIdx = cellsList[i].PrevIdx = -1;
        }
    }

    /**
     * 获取所有空位列表
     * @param res 结果数组
     * @return 空位数量
     */
    public int getEmptyPointsList(int[] res) {
        PointNode node = cellsList[cellsList[HEAD].NextIdx];
        int sz = 0;
        while (node.NextIdx != -1) {
            res[sz++] = node.Key;
            node = cellsList[node.NextIdx];
        }
        return sz;
    }

    /**
     * 初始化链表 - 所有位置都加入空列表
     */
    public void init() {
        for (int i = 0; i < cellsList.length; i++) {
            cellsList[i].NextIdx = cellsList[i].PrevIdx = -1;
        }
        cellsList[HEAD].NextIdx = TAIL;
        cellsList[TAIL].NextIdx = -1;
        cellsList[HEAD].PrevIdx = -1;
        cellsList[TAIL].PrevIdx = HEAD;

        // 将所有棋盘位置加入链表
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                addCell(i, j);
            }
        }

        // 清空计数器
        for (int i = 0; i < cellsCnt.length; i++) {
            cellsCnt[i] = 0;
        }
    }

    /**
     * 从链表中删除一个位置（落子时调用）
     * @param x 棋盘X坐标
     * @param y 棋盘Y坐标
     */
    public void removeCell(int x, int y) {
        int key = x * GameConstants.ChessBorderNum + y;
        if (cellsList[key].PrevIdx != -1 || cellsList[key].NextIdx != -1) {
            cellsList[cellsList[key].PrevIdx].NextIdx = cellsList[key].NextIdx;
            cellsList[cellsList[key].NextIdx].PrevIdx = cellsList[key].PrevIdx;
            cellsList[key].PrevIdx = cellsList[key].NextIdx = -1;
        }
    }

    /**
     * 将位置加入链表（悔棋时调用）
     * @param x 棋盘X坐标
     * @param y 棋盘Y坐标
     */
    public void addCell(int x, int y) {
        int key = x * GameConstants.ChessBorderNum + y;
        if (cellsList[key].NextIdx == -1 || cellsList[key].PrevIdx == -1) {
            cellsList[key].PrevIdx = HEAD;
            cellsList[key].NextIdx = cellsList[HEAD].NextIdx;
            cellsList[cellsList[HEAD].NextIdx].PrevIdx = key;
            cellsList[HEAD].NextIdx = key;
        }
    }

    /**
     * 增加邻近棋子计数并将空位加入列表
     * @param x 基准位置X坐标
     * @param y 基准位置Y坐标
     * @param panel 棋盘状态
     * @param direction 方向数组
     */
    public void addCellsAround(int x, int y, byte[][] panel, int[][] direction) {
        for (int i = 0; i < 4; i++) {
            int dx = direction[i][0], dy = direction[i][1];
            for (int j = 1; j <= 2; j++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int tx = x + j * dx * sign;
                    int ty = y + j * dy * sign;
                    if (isValidPosition(tx, ty) && panel[tx][ty] == 0) {
                        int key = tx * GameConstants.ChessBorderNum + ty;
                        if (cellsCnt[key] == 0) {
                            addCell(tx, ty);
                        }
                        cellsCnt[key]++;
                    }
                }
            }
        }
    }

    /**
     * 减少邻近棋子计数并从列表中删除空位
     * @param x 基准位置X坐标
     * @param y 基准位置Y坐标
     * @param panel 棋盘状态
     * @param direction 方向数组
     */
    public void removeCellsAround(int x, int y, byte[][] panel, int[][] direction) {
        for (int i = 0; i < 4; i++) {
            int dx = direction[i][0], dy = direction[i][1];
            for (int j = 1; j <= 2; j++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int tx = x + j * dx * sign;
                    int ty = y + j * dy * sign;
                    if (isValidPosition(tx, ty)) {
                        int key = tx * GameConstants.ChessBorderNum + ty;
                        if (panel[tx][ty] == 0 && cellsCnt[key] > 0) {
                            cellsCnt[key]--;
                            if (cellsCnt[key] == 0) {
                                removeCell(tx, ty);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查坐标是否在棋盘范围内
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < GameConstants.ChessBorderNum 
            && y >= 0 && y < GameConstants.ChessBorderNum;
    }
}
