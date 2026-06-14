
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.BoardUtils;
import utils.GameConstants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestNeighbor {
    private final static int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};


    private final int border = GameConstants.ChessBorderNum; // 15 (0到14)

    @Test
    @DisplayName("🧪 边界测试 1：左上角顶点 (0,0) 一维数据对齐验证")
    public void testTopLeftCorner() {
        TestNeighbor tester = new TestNeighbor();
        byte[][] panel = new byte[border][border];
        panel[0][0] = 1;

        // 💡 物理对齐：x=0, y=0 -> 0*15+0 = 0
        Map<Integer, Integer> activePointsMap = Map.of(0, 1);

        assertDoesNotThrow(() -> tester.Neighbor(panel, new HashMap<>(activePointsMap)));

        HashMap<Integer, Integer> result = tester.Neighbor(panel, new HashMap<>(activePointsMap));
        assertTrue(result.containsKey(1 * border + 0), "❌ 漏防：未抓到右侧邻居(1,0)");
        assertTrue(result.containsKey(0 * border + 1), "❌ 漏防：未抓到下方邻居(0,1)");
        assertFalse(result.containsKey(0), "❌ 错误：有子的格子本身不能当邻居！");
    }

    @Test
    @DisplayName("🧪 边界测试 2：右下角边缘 (14,14) 一维数据对齐验证")
    public void testBottomRightCorner() {
        TestNeighbor tester = new TestNeighbor();
        byte[][] panel = new byte[border][border];
        panel[14][14] = 1;

        // 💡 物理对齐：x=14, y=14 -> 14*15+14 = 224
        Map<Integer, Integer> activePointsMap = Map.of(14 * border + 14, 1);

        HashMap<Integer, Integer> result = tester.Neighbor(panel, new HashMap<>(activePointsMap));
        assertTrue(result.containsKey(13 * border + 14), "❌ 漏防：未靠反向减法抓到左边邻居");
        assertTrue(result.containsKey(14 * border + 13), "❌ 漏防：未靠反向减法抓到上边邻居");
    }

    @Test
    @DisplayName("🧪 密集搏杀测试 3：同一行多子贴身重叠 (7,7) 和 (7,8) 完美通过验证")
    public void testMultiChessOverlap() {
        TestNeighbor tester = new TestNeighbor();
        byte[][] panel = new byte[border][border];

        // 模拟同一行两个子贴身：(7,7) 和 (7,8)
        panel[7][7] = 1;
        panel[7][8] = 2;

        // ✨ 奇迹发生：因为一维降维了，它们在 Map 里的 Key 分别是 112 和 113！
        // Key 绝不冲突，Map.of 极其安全、清爽地直接塞入，完美模拟真实对局！
        Map<Integer, Integer> activePointsMap = Map.of(
                7 * border + 7, 1, // Key: 112
                7 * border + 8, 1  // Key: 113
        );

        HashMap<Integer, Integer> result = tester.Neighbor(panel, new HashMap<>(activePointsMap));

        // 断言：它们两个之间公共交叉、连成一片的邻居点必须安全去重合并
        assertTrue(result.containsKey(8 * border + 7), "❌ 错误：贴身交界处的重叠有效点丢失了！");

        // 占位防线断言：不管多近，有棋子的格子绝对要被挡在外边
        assertFalse(result.containsKey(7 * border + 7), "❌ 致命漏洞：(7,7)已有子，不能当做空格邻居！");
        assertFalse(result.containsKey(7 * border + 8), "❌ 致命漏洞：(7,8)已有子，不能当做空格邻居！");
    }

    //获取单点邻居
    private void GetNeighbor(byte[][] panel, HashMap<Integer, Integer> neighbor, int x, int y) {
        if (!BoardUtils.IsLegal(x, y) || panel[x][y] == 0) return;
        int border = GameConstants.ChessBorderNum;
        //遍历4个方向
        for (int i = 0; i < 4; i++) {
            int dx = Direction[i][0], dy = Direction[i][1];
            int k = 1;
            while (k <= 2) {
                // 1. 正方向延伸 (+k)
                int xx1 = x + dx * k, yy1 = y + dy * k;
                if (BoardUtils.IsLegal(xx1, yy1) && panel[xx1][yy1] == 0) {
                    neighbor.put(xx1 * border + yy1, 1);
                }

                // 2. 反方向延伸 (-k)
                int xx2 = x - dx * k, yy2 = y - dy * k;
                if (BoardUtils.IsLegal(xx2, yy2) && panel[xx2][yy2] == 0) {
                    neighbor.put(xx2 * border + yy2, 1);
                }
                k++;
            }
        }
    }

    private HashMap<Integer, Integer> Neighbor(byte[][] panel, HashMap<Integer, Integer> activePointsMap) {
        HashMap<Integer, Integer> neighbor = new HashMap<>();
        int border = GameConstants.ChessBorderNum;

        for (Map.Entry<Integer, Integer> entry : activePointsMap.entrySet()) {
            int keyCode = entry.getKey();

            int x = keyCode / border; // 112 / 15 = 7
            int y = keyCode % border; // 112 % 15 = 7

            GetNeighbor(panel, neighbor, x, y);
        }
        return neighbor;
    }
}
