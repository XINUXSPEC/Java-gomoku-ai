
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.BoardUtils;
import utils.GameConstants;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestGetBestPosition {
    private final static int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};
    private byte AI_COLOR=2;
    private byte PLAYER_COLOR=1;
    private int size=15;
    static final int DEEP = 2;
    static private int deep = DEEP;
    static double CenterWeight = 0.80;
    static double CenterScore = 50;


    @Test
    @DisplayName("测试：空棋盘时，AI应该能返回一个合法的落子位置")
    void testEmptyPanel() {
        byte[][] panel = new byte[size][size];

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        assertNotNull(bestPos, "最佳位置不应为 null");
        assertTrue(bestPos.x >= 0 && bestPos.x < size, "横坐标应在棋盘内");
        assertTrue(bestPos.y >= 0 && bestPos.y < size, "纵坐标应在棋盘内");
    }

    @Test
    @DisplayName("测试：AI 进攻（活四）- 应该直接选择获胜点（成五）")
    void testAIAttackWin() {
        byte[][] panel = new byte[size][size];

        // 假设在第 3 行，AI 已经连续落了 4 子: (3,1), (3,2), (3,3), (3,4)
        // 期望 AI 在 (3,5) 或 (3,0) 落子以赢得比赛
        int testRow = 3;
        panel[testRow][1] = AI_COLOR;
        panel[testRow][2] = AI_COLOR;
        panel[testRow][3] = AI_COLOR;
        panel[testRow][4] = AI_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        // 验证 AI 是否落在了制胜点上
        assertTrue((bestPos.x == testRow && bestPos.y == 5) || (bestPos.x == testRow && bestPos.y == 0),
                "AI 未能在活四局面下选择直接获胜的落子点。实际落子: " + bestPos);
    }

    @Test
    @DisplayName("测试：AI 防守 - 玩家有活三/活四时，AI 应前往拦截")
    void testAIDefend() {
        byte[][] panel = new byte[size][size];

        // 假设玩家在斜线上有连续 3 个子：(2,2), (3,3), (4,4)
        // 危险期，AI 应该去拦截 (1,1) 或 (5,5)
        panel[2][2] = PLAYER_COLOR;
        panel[3][3] = PLAYER_COLOR;
        panel[4][4] = PLAYER_COLOR;
        panel[5][5] = AI_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println(bestPos);
        // 验证是否进行了有效拦截
        assertTrue((bestPos.x == 1 && bestPos.y == 1) || (bestPos.x == 5 && bestPos.y == 5),
                "AI 未能有效拦截玩家的连续攻势。实际落子: " + bestPos);
    }

    @Test
    @DisplayName("测试：满棋盘无处落子（极端边界情况）")
    void testFullPanel() {
        byte[][] panel = new byte[size][size];

        // 将棋盘填满
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                panel[i][j] = PLAYER_COLOR;
            }
        }

        // 此时所有 panel[i][j] == 0 的条件都不满足
        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        // 根据原代码逻辑，若进不去 if(panel[i][j] == 0)，将返回初始化的 Point(0,0)
        assertEquals(0, bestPos.x);
        assertEquals(0, bestPos.y);
    }

    @Test
    @DisplayName("测试：双三/四三进攻抉择 - 存在获胜点时，AI是否能识别并落子")
    void testDoubleThreeAttack() {
        byte[][] panel = new byte[size][size];

        // 构造棋子
        panel[7][5] = AI_COLOR;
        panel[7][6] = AI_COLOR;
        panel[7][7] = AI_COLOR; // 交叉点
        panel[6][7] = AI_COLOR;
        panel[5][7] = AI_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        // 修正后的断言：
        // 横向活三的延伸点：(7,4) 或 (7,8)
        // 纵向活三的延伸点：(4,7) 或 (8,7) -> 你的AI选了(8,7)，完全正确！
        boolean isValidAttack = (bestPos.x == 7 && bestPos.y == 8) ||
                (bestPos.x == 7 && bestPos.y == 4) ||
                (bestPos.x == 4 && bestPos.y == 7) ||
                (bestPos.x == 8 && bestPos.y == 7);

        assertTrue(isValidAttack, "AI 未能在双重威胁盘面上选择最优进攻点。实际落子: " + bestPos);
    }

    @Test
    @DisplayName("测试：防守优先级 - 玩家同时有活三和活二，AI 应优先堵活三")
    void testDefensePriority() {
        byte[][] panel = new byte[size][size];

        // 威胁 1：玩家有活三（高威胁），在第 2 行：(2,3), (2,4), (2,5)
        panel[2][3] = PLAYER_COLOR;
        panel[2][4] = PLAYER_COLOR;
        panel[2][5] = PLAYER_COLOR;

        // 威胁 2：玩家有活二（低威胁），在第 8 行：(8,8), (8,9)
        panel[8][8] = PLAYER_COLOR;
        panel[8][9] = PLAYER_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        // AI 必须去堵第 2 行的活三，落子在 (2,2) 或 (2,6)
        assertTrue(bestPos.x == 2 && (bestPos.y == 2 || bestPos.y == 6),
                "AI 没有优先去堵玩家的活三，而是被其他低价值棋子分散了注意力。实际落子: " + bestPos);
    }

    @Test
    @DisplayName("测试：死四 vs 活三 - 评估函数对受堵棋局的判分逻辑")
    void testBlockedFourVsActiveThree() {
        byte[][] panel = new byte[size][size];

        // 玩家有一条被堵住一头的“冲四/死四”：(4,2)是AI的，(4,3)(4,4)(4,5)(4,6)是玩家的
        // 此时玩家只要在 (4,7) 落子就直接成五获胜。
        panel[4][2] = AI_COLOR;
        panel[4][3] = PLAYER_COLOR;
        panel[4][4] = PLAYER_COLOR;
        panel[4][5] = PLAYER_COLOR;
        panel[4][6] = PLAYER_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        // 即使这一头被堵住了，AI 这一步也必须死守 (4,7)，否则必输
        assertEquals(4, bestPos.x, "AI 必须在第 4 行进行防守");
        assertEquals(7, bestPos.y, "AI 必须堵住死四的唯一生路 (4,7)");
    }

    @Test
    @DisplayName("测试：性能与耗时 - 评估单次决策的计算效率")
    void testPerformanceTime() {
        byte[][] panel = new byte[size][size];

        // 随机在棋盘上放一些棋子，模拟复杂的真实中局
        for (int i = 0; i < size; i += 2) {
            for (int j = 0; j < size; j += 3) {
                panel[i][j] = (byte) ((i + j) % 2 == 0 ? AI_COLOR : PLAYER_COLOR);
            }
        }

        // 记录执行时间
        long startTime = System.currentTimeMillis();
        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        System.out.println("AI 决策耗时: " + duration + " ms");

        assertNotNull(bestPos);
        // 通常未开启深度极大极小搜索（当前仅 1 层遍历）时，耗时应在 200ms 以内
        // 如果这里超时，说明 eval 方法里的循环有性能瓶颈（如之前提到的 while 边界判断问题）
        assertTrue(duration < 200, "AI 决策耗时过长 (" + duration + "ms)，可能存在死循环或性能瓶颈！");
    }

    @Test
    @DisplayName("测试：人类玩家冲四，AI 必须立刻拦截")
    void testHumanStraightFourDefense() {
        byte[][] panel = new byte[size][size];

        // 构造场景：人类玩家在主对角线上放置了 4 颗连续的棋子
        // 棋子位置：(3,3), (4,4), (5,5), (6,6)
        panel[3][3] = PLAYER_COLOR;
        panel[4][4] = PLAYER_COLOR;
        panel[5][5] = PLAYER_COLOR;
        panel[6][6] = PLAYER_COLOR;

        // 此时，人类玩家只要下在 (2,2) 或者 (7,7) 就能直接成五获胜。
        // 由于你的代码中评价函数对 4 连子的判分极大（RuleScore(4) = 4000），
        // 且 eval 计算的是对方得分（eval1），AI 为了让最后的 (eval2 - eval1) 最大，
        // 必须通过抢占 (2,2) 或 (7,7) 来打破人类的这个高分五元组。

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);

        System.out.println("玩家冲四时，AI选择的拦截点: " + bestPos);

        // 验证 AI 是否成功拦截
        boolean isInterceptionSuccessful = (bestPos.x == 2 && bestPos.y == 2) ||
                (bestPos.x == 7 && bestPos.y == 7);

        assertTrue(isInterceptionSuccessful,
                "大危机！人类玩家已经冲四，但 AI 未能前往 (2,2) 或 (7,7) 进行拦截！实际落子: " + bestPos);
    }

    //到此
    @Test
    @DisplayName("情况一：人类 1 子 vs AI 0 子")
    void testSituation1() {
        byte[][] panel = new byte[size][size];
        // 人类在正中心 (7,7) 下了第一步棋
        panel[7][7] = PLAYER_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【人类1子】AI选择落子在: " + bestPos);

        // 理论上：面对人类的单子，AI 最佳的选择是紧贴着人类落子（比如 7,8 或 8,7 或 6,6），限制其发展
        double distance = Math.sqrt(Math.pow(bestPos.x - 7, 2) + Math.pow(bestPos.y - 7, 2));
        assertTrue(distance <= 1.5, "人类刚下一子，AI 应该贴身防守或占领中心，不该下得太远！");
    }

    @Test
    @DisplayName("情况二：人类活二 vs AI活一")
    void testSituation2() {

        byte[][] panel = new byte[size][size];

        // 人类有活二：(5,5), (5,6)
        panel[5][5] = PLAYER_COLOR;
        panel[5][6] = PLAYER_COLOR;

        // AI 有活一：(9,9)
        panel[9][9] = AI_COLOR;

        Point bestPos = getBestPosition(panel, AI_COLOR);
        System.out.println("【人类活二 vs AI活一】AI落子: " + bestPos);

        // 抉择分析：人类的活二开始有威胁了，而AI只有活一。
        // AI 应该优先选择去堵人类的活二（落子在 5,4 或 5,7），或者在自己活一的基础上扩展成活二（如 9,10）。
        // 通常防守活二的优先级会高一点点。
        boolean blockedHuman = (bestPos.x == 5 && bestPos.y == 4) || (bestPos.x == 5 && bestPos.y == 7);
        assertTrue(blockedHuman, "大危机！人类已经活三，AI 的活二无法对轰，必须优先防守人类的活三！");
    }

    @Test
    @DisplayName("情况三：人类活三 vs AI活二")
    void testSituation3() {
        byte[][] panel = new byte[size][size];

        // 人类有活三：(5,4), (5,5), (5,6)
        panel[5][4] = PLAYER_COLOR;
        panel[5][5] = PLAYER_COLOR;
        panel[5][6] = PLAYER_COLOR;

        // AI 有活二：(9,8), (9,9)
        panel[9][8] = AI_COLOR;
        panel[9][9] = AI_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【人类活三 vs AI活二】AI落子: " + bestPos);

        // 抉择分析：人类的活三极其危险（下一步变活四就必输了）。而AI自己的活二就算再下一子也只是活三。
        // 结论：AI 必须选择防守！去堵人类的活三，落子在 (5,3) 或 (5,7)。
        boolean blockedHuman = (bestPos.x == 5 && bestPos.y == 3) || (bestPos.x == 5 && bestPos.y == 7);
        //assertTrue(blockedHuman, "大危机！人类已经活三，AI 的活二无法对轰，必须优先防守人类的活三！");

        // ====================================================
        // 2. 获取下在 (5,3) 位置[防守点]的分数
        // ====================================================
        panel[5][3] = AI_COLOR; // 虚拟落子
        double scoreDefense = TestGetBestPosition.evaluate(panel, AI_COLOR);
        panel[5][3] = 0;        // 恢复盘面

        // ====================================================
        // 3. 获取下在 (9,10) 位置[进攻点]的分数
        // ====================================================
        panel[9][10] = AI_COLOR; // 虚拟落子
        double scoreAttack = TestGetBestPosition.evaluate(panel, AI_COLOR);
        panel[9][10] = 0;        // 恢复盘面


        // ====================================================
        // 4. 打印分数进行量化对比
        // ====================================================
        System.out.println("\n------- 核心位置分数微观对比 -------");
        System.out.println("下在防守点 (5,3) [阻断对方活三] 的局势得分: " + scoreDefense);
        System.out.println("下在进攻点 (9,10) [形成自己活三] 的局势得分: " + scoreAttack);
        System.out.println("-----------------------------------");


        // 辅助断言：理智的 AI 必须让防守得分大于进攻得分
        assertTrue(scoreDefense > scoreAttack,
                String.format("【权重异常】防守分(%.1f) 小于或等于 进攻分(%.1f)，AI 会漏防！", scoreDefense, scoreAttack));
    }

    @Test
    @DisplayName("情况四：人类冲四 vs AI活三")
    void testSituation4() {
        byte[][] panel = new byte[size][size];

        // 人类已经冲四：(5,3), (5,4), (5,5), (5,6) -> 只要在 (5,7) 或 (5,2) 落子就赢了
        panel[5][3] = PLAYER_COLOR;
        panel[5][4] = PLAYER_COLOR;
        panel[5][5] = PLAYER_COLOR;
        panel[5][6] = PLAYER_COLOR;

        // AI 有活三：(9,7), (9,8), (9,9)
        panel[9][7] = AI_COLOR;
        panel[9][8] = AI_COLOR;
        panel[9][9] = AI_COLOR;

        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【人类冲四 vs AI活三】AI落子: " + bestPos);

        // 抉择分析：人类再下一子直接成五（游戏结束）。AI 如果去进攻自己的活三变成活四，依然慢了一步。
        // 结论：AI 必须无条件去堵人类的冲四！落子在 (5,2) 或 (5,7)。
        boolean blockedFour = (bestPos.x == 5 && bestPos.y == 2) || (bestPos.x == 5 && bestPos.y == 7);
        assertTrue(blockedFour, "致命错误！人类已经冲四，AI 绝不能贪恋自己的活三进攻，必须立刻去堵截！");

        // ====================================================
        // 2. 获取下在 (5,7) 位置[防守点]的分数
        // ====================================================
        panel[5][7] = AI_COLOR; // 虚拟落子
        double scoreDefense = TestGetBestPosition.evaluate(panel, AI_COLOR);
        panel[5][7] = 0;        // 恢复盘面


        // ====================================================
        // 3. 获取下在 (9,10) 位置[进攻点]的分数
        // ====================================================
        panel[9][10] = AI_COLOR; // 虚拟落子
        double scoreAttack = TestGetBestPosition.evaluate(panel, AI_COLOR);
        panel[9][10] = 0;        // 恢复盘面


        // ====================================================
        // 4. 打印分数进行量化对比
        // ====================================================
        System.out.println("\n------- 情况四：核心位置分数微观对比 -------");
        System.out.println("下在防守点 (5,7) [阻断对方冲四] 的局势得分: " + scoreDefense);
        System.out.println("下在进攻点 (9,10) [形成自己活四] 的局势得分: " + scoreAttack);
        System.out.println("-------------------------------------------");

        // 强力断言：面对冲四，防守得分必须压倒性地大于进攻得分
        assertTrue(scoreDefense > scoreAttack,
                String.format("【致命缺陷】防守冲四的分数(%.1f) 竟然不如 进攻分数(%.1f)！AI 将会漏防导致直接输棋！",
                        scoreDefense, scoreAttack));
    }

    @Test
    @DisplayName("同盘对比一：(2,2) 变为 (3,3) 组合点 vs 孤立单三点")
    void testSamePanelDoubleThree() {
        byte[][] panel = new byte[size][size];

        // --- 区域 A：构造一个黄金交叉基础 ---
        // 如果 AI 下在 (5,5)，横向会变成 3 子，纵向也会变成 3 子，从而实现 (2,2) -> (3,3) 的双三跃升
        panel[5][3] = AI_COLOR; panel[5][4] = AI_COLOR; // 横向原本2子
        panel[3][5] = AI_COLOR; panel[4][5] = AI_COLOR; // 纵向原本2子

        // --- 区域 B：构造一个孤立的、没有多方向加成的位置 ---
        // 如果 AI 下在 (10,12)，只会让这一行变成单纯的 3 子，没有任何多方向组合
        panel[10][10] = AI_COLOR; panel[10][11] = AI_COLOR;

        // 让 AI 进行抉择
        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【(2,2)->(3,3)同盘博弈】AI 最终选择落子在: " + bestPos);

        // 断言：由于 (5,5) 能够拿到“不同方向两个规则三加100分”的特殊红利，
        // AI 在同一个盘面上绝对应该放弃孤立点，死死咬住交叉点 (5,5)
        assertEquals(5, bestPos.x, "AI 没能顶住诱惑，错过了同盘双三交叉点的横坐标！");
        assertEquals(5, bestPos.y, "AI 没能顶住诱惑，错过了同盘双三交叉点的纵坐标！");
    }

    @Test
    @DisplayName("同盘对比二：(3,2) 变为 (4,3) 绝杀点 vs 孤立冲四点")
    void testSamePanelFourThree() {
        byte[][] panel = new byte[size][size];

        // --- 区域 A：构造四三绝杀基础 ---
        // 如果 AI 下在 (6,6)：
        // 横向：(6,3),(6,4),(6,5) 原本 3 子，落子后变 4 子
        // 纵向：(4,6),(5,6) 原本 2 子，落子后变 3 子
        // 这是一个完美的 (3,2) 变为 (4,3) 拥有 400 分额外加成的绝杀点！
        panel[6][3] = AI_COLOR; panel[6][4] = AI_COLOR; panel[6][5] = AI_COLOR;
        panel[4][6] = AI_COLOR; panel[5][6] = AI_COLOR;

        // --- 区域 B：构造一个孤立的冲四点 ---
        // 如果 AI 下在 (12,12)，也只能让这里变成一个孤立的 4 子（冲四），没有其他方向加成
        panel[12][9] = AI_COLOR; panel[12][10] = AI_COLOR; panel[12][11] = AI_COLOR;

        // 让 AI 进行抉择
        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【(3,2)->(4,3)同盘博弈】AI 最终选择落子在: " + bestPos);

        // 断言：(6,6) 带来的战略价值（成四带活三加400分）远超孤立的冲四
        assertEquals(6, bestPos.x, "AI 漏算了同盘(4,3)绝杀点的横坐标！");
        assertEquals(6, bestPos.y, "AI 漏算了同盘(4,3)绝杀点的纵坐标！");
    }

    @Test
    @DisplayName("同盘对比三：斜向交叉 (2,2) -> (3,3) 双三验证")
    void testSamePanelDiagonalDoubleThree() {
        byte[][] panel = new byte[size][size];

        // 这一次我们用更隐蔽的【斜向交叉】来考校 AI
        // 目标交叉点在 (7,7)
        // 正斜向（Direction[0]）：(5,5), (6,6) -> 原本 2 子
        panel[5][5] = AI_COLOR; panel[6][6] = AI_COLOR;

        // 反斜向（Direction[1]）：(9,5), (8,6) -> 原本 2 子
        panel[9][5] = AI_COLOR; panel[8][6] = AI_COLOR;

        // 同盘干扰项：在 (14,2) 处放一个普通的活二，下在 (14,3) 只能凑一个孤立的单三
        panel[14][2] = AI_COLOR; panel[14][3] = AI_COLOR;

        // 让 AI 进行抉择
        Point bestPos = TestGetBestPosition.getBestPosition(panel, AI_COLOR);
        System.out.println("【斜向(2,2)->(3,3)同盘博弈】AI 最终选择落子在: " + bestPos);

        // 断言：斜向交叉同样受不同方向加成规则保护，AI 必须落在 (7,7)
        assertEquals(7, bestPos.x, "AI 对斜向交叉的组合加成不敏感！横坐标错误。");
        assertEquals(7, bestPos.y, "AI 对斜向交叉的组合加成不敏感！纵坐标错误。");
    }

    @Test
    @DisplayName("⏱️ 性能专项压测：高精度计算耗时与诊断断言")
    void testGetBestPositionPerformance() {
        String CYAN = "\u001B[36m";
        String RESET = "\u001B[0m";
        String GREEN = "\u001B[32m";
        String RED = "\u001B[31m";

        System.out.println(CYAN + "==================================================" + RESET);
        System.out.println(CYAN + "     ⏱️ 五子棋 AI getBestPosition 耗时深度压测 ⏱️" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);

        byte[][] benchPanel = new byte[size][size];
        // 构造一个中局缠绕盘面（约10个棋子），迫使 AI 进入复杂的博弈树分支
        benchPanel[7][7] = AI_COLOR;     benchPanel[7][8] = PLAYER_COLOR;
        benchPanel[6][8] = AI_COLOR;     benchPanel[8][6] = PLAYER_COLOR;
        benchPanel[5][9] = AI_COLOR;     benchPanel[9][5] = PLAYER_COLOR;
        benchPanel[7][6] = PLAYER_COLOR; benchPanel[6][7] = AI_COLOR;
        benchPanel[4][10] = AI_COLOR;    benchPanel[10][4] = PLAYER_COLOR;

        // 1. 预热 JVM（防止首次加载类和即时编译干扰测试结果）
        for (int i = 0; i < 3; i++) {
            getBestPosition(benchPanel, AI_COLOR);
        }

        // 2. 正式高精度纳秒级耗时统计
        long startTime = System.nanoTime();
        Point bestPos = getBestPosition(benchPanel, AI_COLOR);
        long endTime = System.nanoTime();

        // 纳秒转毫秒
        double durationMillis = (endTime - startTime) / 1_000_000.0;

        System.out.println(" -> AI 决策出的最佳落子点为: " + bestPos);
        System.out.printf(" -> [DEEP = %d] 状态下决策总耗时: " + RED + "%.2f ms" + RESET + "\n", DEEP, durationMillis);

        // 3. 性能硬核断言拦截
        // 在 DEEP = 2 的单层博弈中，现代合格的评估算法耗时绝对不能超过 150ms。
        // 如果你的机器跑出来需要数秒，说明博弈树陷入了严重的无用功。
        assertTrue(durationMillis < 150,
                String.format("【🚨 性能红牌】AI决策效率太低！耗时 %.2f ms，必须进行算法底层优化！", durationMillis));

        System.out.println(GREEN + "🎉 性能测试流程完毕" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);
    }

    static void main(){

    }

    //获取最佳得分位置
    static public Point getBestPosition(byte[][] panel,byte AI){
        Point point = new Point();
        point.y=0;point.x=0;
        int col= GameConstants.ChessBorderNum-1,row=col;
        double score=Integer.MIN_VALUE;
        //遍历位置
        for (int i = 0; i<=col; i++){
            for(int j = 0; j<=row; j++){
                if(panel[i][j] == 0) {
                    panel[i][j]=AI;
                    double t = minimax(panel, AI, false);
                    panel[i][j]=0;
                    if (t >= score) {
                        score = t;
                        point.x = i;
                        point.y = j;
                    }
                }
            }
        }
        return point;
    }

    //博弈树搜索
    static double minimax(byte[][] panel,byte AI,Boolean isMax){
        if(deep==0)     return evaluate(panel, AI);

        byte Player = (byte) (3-AI);
        if (isMax) {
            int n = GameConstants.ChessBorderNum;
            double score = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (panel[i][j] == 0) {
                        panel[i][j] = AI;
                        deep--;
                        score = Double.max(score, minimax(panel, AI, !isMax));
                        deep++;
                        panel[i][j] = 0;
                    }
                }
            }
            return score;
        }else{
            int n = GameConstants.ChessBorderNum;
            double score = -Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (panel[i][j] == 0) {
                        panel[i][j] = Player;
                        deep--;
                        score = Double.min(score, minimax(panel, AI, !isMax));
                        deep++;
                        panel[i][j] = 0;
                    }
                }
            }
            return score;
        }
    }

    //获取当前局面评分
    static double evaluate(byte[][] panel,byte AI){
        double eval1=0,eval2=0;
        byte Player = (byte)(AI==1?2:1);
        //System.out.println("电脑颜色:"+ToolClass.GetColorByFlag(AI)+"玩家颜色:"+ToolClass.GetColorByFlag(Player));
        int col = GameConstants.ChessBorderNum-1,row=GameConstants.ChessBorderNum-1;
        for (int i = 0; i <=row; i++) {
            for (int j = 0; j <=col; j++) {
                if(panel[i][j]!=Player && panel[i][j] != AI)    continue;
                double t=eval(panel,i,j);
                if(panel[i][j] == Player){
                    eval1+=t;
                }else{
                    eval2+=t;
                }
            }
        }
        //System.out.println("玩家得分:"+eval1+"电脑得分:"+eval2);
        return eval2-eval1;
    }

    //获取棋子的得分
    static double eval(byte[][] panel, int i, int j){
        byte flag = panel[i][j], flag1 = (byte)(flag==1?2:1);
        double[] Srore=new double[4];
        int Rule3=0, Rule4=0;

        for (int k = 0; k < 4; k++) {
            double c=0;
            boolean Other=false;
            int top_x = i, top_y=j, dx=Direction[k][0], dy=Direction[k][1];
            while (BoardUtils.IsLegal(top_x+dx, top_y+dy)
                    &&Math.abs(top_x+dx-i)<=4
                    &&Math.abs(top_y+dy-j)<=4) {
                top_x += dx;
                top_y += dy;
            }

            int bot_x=i, bot_y=j;
            while (BoardUtils.IsLegal(bot_x-dx, bot_y-dy)
                    &&Math.abs(bot_x-dx-i)<=4
                    &&Math.abs(bot_y-dy-j)<=4) {
                bot_x -= dx;
                bot_y -= dy;
            }

            if(Math.abs(top_x-bot_x)<4 && Math.abs(top_y-bot_y)<4) continue;

            int x=top_x, y=top_y, tx=x-4*dx, ty=y-4*dy, tt=0;
            int cnt =0;
            while (BoardUtils.IsLegal(tx, ty)){
                int t = 0;
                for (int l = 0; l <= 4; l++) {
                    int mx = x-dx*l, my=y-dy*l;
                    if(panel[mx][my]== flag1){
                        Other=true;
                        break;
                    }else if(panel[mx][my]==flag) t++;
                }

                if(Other){
                    Other=false;
                    x-=dx; y-=dy;
                    tx-=dx; ty-=dy;
                    continue;
                }

                if(t>tt) tt=t;
                c+=RuleScore(t);
                if(x == i && y == j) break;
                x-=dx; y-=dy;
                tx-=dx; ty-=dy;
            }
            if(Other) continue;
            if(tt == 3) Rule3++;
            else if(tt == 4) Rule4++;
            Srore[k]=c;
        }
        double res=0;
        for(double t:Srore) res+=t;

        // 不同方向多组合红利触发规则
        if(Rule4>=2) res+=3000;
        else if(Rule4==1 && Rule3>=1) res+=1000;
        else if(Rule3>=2) res+=400;

        // 引入基于到中心格子的欧几里得距离权重计算
        int mid = GameConstants.ChessBorderNum/2;
        double distance = Math.sqrt((i-mid)*(i-mid)+(j-mid)*(j-mid));
        res+=CenterScore-CenterScore*(1-CenterWeight)*distance;
        return res;
    }

    //根据个数获取分数
    static double RuleScore(int n){
        switch (n){
            case 0: return 0;
            case 1: return 1;
            case 2: return 30;
            case 3: return 600;
            case 4: return 4000;
            default:    return 1000000;
        }
    }

}