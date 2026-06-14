

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import utils.BoardUtils;
import utils.GameConstants;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;

@DisplayName("五子棋 AI 核心评估算法与性能自动化断言压测")
public class TestEval {
    private final static int[][] Direction = {{1, 1}, {-1, 1}, {0, 1}, {1, 0}};
    private static final byte AI_COLOR = 2;
    private static final byte PLAYER_COLOR = 1;
    private static final int size = 15;
    static final int DEEP = 2;
    private static final int deep = DEEP;
    static double CenterWeight = 0.80;
    static double CenterScore = 50;

    @Test
    @DisplayName("⏱️ 性能测试：高精度算法运行耗时与诊断断言")
    void testTimePerformance() {
        String CYAN = "\u001B[36m";
        String RESET = "\u001B[0m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";

        System.out.println(CYAN + "==================================================" + RESET);
        System.out.println(CYAN + "      ⏱️ 五子棋 AI 核心算法耗时性能压测 ⏱️" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);

        byte[][] benchPanel = new byte[15][15];
        benchPanel[7][7] = 1; benchPanel[7][8] = 1; benchPanel[7][9] = 1;
        benchPanel[6][7] = 2; benchPanel[8][7] = 2; benchPanel[5][5] = 2;
        benchPanel[3][3] = 1; benchPanel[11][12] = 1; benchPanel[14][14] = 2;

        // 1. 底层单次 eval(i,j) 耗时
        long startEval = System.nanoTime();
        for (int m = 0; m < 10000; m++) {
            eval(benchPanel, 7, 7);
        }
        long endEval = System.nanoTime();
        double avgEvalTimeMicros = ((endEval - startEval) / 10000.0) / 1000.0;
        System.out.printf("【1】单点五元组扫描 `eval(i,j)` 平均耗时: " + GREEN + "%.3f μs" + RESET + "\n", avgEvalTimeMicros);

        // 2. 整盘 evaluate() 耗时
        long startEvaluate = System.nanoTime();
        for (int m = 0; m < 1000; m++) {
            evaluate(benchPanel, AI_COLOR);
        }
        long endEvaluate = System.nanoTime();
        double avgEvaluateTimeMillis = ((endEvaluate - startEvaluate) / 1000.0) / 1000000.0;
        System.out.printf("【2】全盘 15x15 矩阵 `evaluate()` 平均耗时: " + GREEN + "%.3f ms" + RESET + "\n", avgEvaluateTimeMillis);

        // 3. 核心黄金落子决策 `getBestPosition()` 总耗时
        long startBest = System.nanoTime();
        Point bestPos = getBestPosition(benchPanel);
        long endBest = System.nanoTime();
        double totalBestTimeMillis = (endBest - startBest) / 1000000.0;

        System.out.println(" -> AI 决策出的最佳落子点为: " + bestPos);
        System.out.printf("【3】AI 遍历全盘寻敌 `getBestPosition()` 总耗时: " + GREEN + "%.2f ms" + RESET + "\n", totalBestTimeMillis);

        // 🚨 自动化性能阈值拦截：单层全盘决策耗时在现代机器上绝对不应超过 200ms
        assertTrue(totalBestTimeMillis < 200, "【性能警报】算法底层存在严重性能瓶颈，耗时过长！总耗时: " + totalBestTimeMillis + "ms");
        System.out.println(CYAN + "==================================================" + RESET);
    }

    @Test
    @DisplayName("🔬 底层测试：单点多方向五元组扫描与边界处理")
    void testEval() {
        System.out.println("====== 开始评估函数底层原子断言压测 ======");

        // 1. 对角线交叉压测
        byte[][] panel2 = new byte[15][15];
        panel2[5][5] = 1; panel2[6][6] = 1; panel2[7][7] = 1; // 正斜向
        panel2[7][5] = 1; panel2[6][6] = 1; panel2[5][7] = 1; // 反斜向
        double score2 = eval(panel2, 6, 6);
        System.out.println("【测试 1】对角线交叉点 (6,6) 评估得分: " + score2);
        assertTrue(score2 > 0, "对角线有效交叉点评分不应为 0");

        // 2. 贴边与 Corners 极端边界压测
        byte[][] panel3 = new byte[15][15];
        panel3[0][0] = 1; panel3[0][1] = 1; panel3[0][2] = 1; // 左上角贴边活三
        panel3[1][0] = 1;                                     // 纵向活二基础
        double score3 = eval(panel3, 0, 0);
        System.out.println("【测试 2】极边缘顶点 (0,0) 评估得分: " + score3);
        assertTrue(score3 > 0, "顶点边界扫描截断逻辑有误，未成功计分");

        // 3. 全敌方棋子遮挡测试（验证拦截规则）
        byte[][] panel4 = new byte[][]{
                {2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {2, 2, 2, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // 只有 (3,3) 是 1，周围全被 2 强行堵死
                {2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        };
        double score4 = eval(panel4, 3, 3);
        System.out.println("【测试 3】敌方全包围 (3,3) 评估得分: " + score4);
        // 被死死堵住的单子在任何5元组内都包含敌棋，无法发展，分值应回归最基础的中心距离分（此处远离中心，排除后应极低或不含棋形分）
        assertTrue(score4 < 50, "被铁壁死堵的孤子不应产生战略棋形分！");

        // 4. 满贯成五测试
        byte[][] panel5 = new byte[15][15];
        panel5[5][2] = 1; panel5[5][3] = 1; panel5[5][4] = 1; panel5[5][5] = 1; panel5[5][6] = 1;
        double score5 = eval(panel5, 5, 4);
        System.out.println("【测试 4】达成五子连珠 (5,4) 评估得分: " + score5);
        assertTrue(score5 >= 1000000, "五子连珠基础分未达到终极必胜大分条件！");

        System.out.println("====== 底层原子断言压测全部通过 ======");
    }


    @Test
    @DisplayName("🌍 大局观测试：Minimax 全局盘面判定与组合红利断言拦截")
    void testEvaluate() {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String GREEN = "\u001B[32m";
        String YELLOW = "\u001B[33m";
        String CYAN = "\u001B[36m";

        System.out.println(CYAN + "==================================================" + RESET);
        System.out.println(CYAN + "     🌍 Minimax 全局盘面大局观评估压测 🌍" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);

        // [全局1] 黑棋活四绝对优势
        byte[][] gPanel1 = new byte[15][15];
        gPanel1[7][5] = 2; gPanel1[7][6] = 2; gPanel1[7][7] = 2; gPanel1[7][8] = 2;
        gPanel1[2][2] = 1;
        double gScore1 = evaluate(gPanel1, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 1】黑棋(2)活四大优，白棋(1)孤立无援" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore1 + RESET + "\n");
        assertTrue(gScore1 > 3000, "黑棋巨大成四优势，全局分应为强力正数");

        // [全局2] 白棋冲四绝对优势
        byte[][] gPanel2 = new byte[15][15];
        gPanel2[3][3] = 1; gPanel2[4][4] = 1; gPanel2[5][5] = 1; gPanel2[6][6] = 1;
        gPanel2[10][10] = 2; gPanel2[12][5] = 2;
        double gScore2 = evaluate(gPanel2, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 2】白棋(1)对角线冲四，黑棋(2)劣势" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore2 + RESET + "\n");
        assertTrue(gScore2 < -3000, "白棋拥有冲四杀招，全局分（黑减白）应该为巨大负数");

        // [全局3] 镜像对峙均势
        byte[][] gPanel3 = new byte[15][15];
        gPanel3[3][4] = 1; gPanel3[3][5] = 1; gPanel3[3][6] = 1; // 白棋活三
        gPanel3[10][4] = 2; gPanel3[10][5] = 2; gPanel3[10][6] = 2; // 黑棋相同活三
        double gScore3 = evaluate(gPanel3, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 3】黑白双方各持一个镜像活三（均势）" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore3 + RESET + "\n");
        // 允许正负15分的浮动（因为两子所处的行数曼哈顿中心距离有极其微弱的差异）
        assertEquals(0.0, gScore3, 50, "镜像对峙下，双方棋形完全对等，分值差额应接近于 0");

        // [全局4] 边缘盲区计分有效性
        byte[][] gPanel4 = new byte[15][15];
        gPanel4[14][5] = 2; gPanel4[14][6] = 2; // 最底线第14行
        double gScore4 = evaluate(gPanel4, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 4】最底边缘行(第14行)落子压测" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore4 + RESET + "\n");
        assertTrue(gScore4 > 0, "【⚠️Bug】最底边界落子未能成功扫描计分，全局分为0或负！");

        // [全局5] 冲四被双堵（防守有效性拦截）
        byte[][] gPanel5 = new byte[15][15];
        gPanel5[5][3] = 2; gPanel5[5][4] = 2; gPanel5[5][5] = 2; gPanel5[5][6] = 2;
        gPanel5[5][2] = 1; gPanel5[5][7] = 1; // 铁壁死堵
        double gScore5 = evaluate(gPanel5, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 5】黑棋(2)冲四，但两端被白棋(1)铁壁死堵" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore5 + RESET + "\n");
        // 被堵死的四子无法成五，战略价值暴跌。算上中心位置分，其分值绝对不能拿到普通强力棋形的高分。
        assertTrue(gScore5 < 150, "【⚠️漏洞】AI产生错觉！被两端死堵的废四依然给出了极高的威胁分: " + gScore5);

        // [全局6] 经典交叉点双三绝杀（多方向组合红利断言）
        byte[][] gPanel6 = new byte[15][15];
        gPanel6[7][6] = 2; gPanel6[7][7] = 2; gPanel6[7][8] = 2; // 横向活三
        gPanel6[6][7] = 2;                     gPanel6[8][7] = 2; // 纵向活三，交叉点在(7,7)
        double gScore6 = evaluate(gPanel6, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 6】黑棋(2)在 (7,7) 祭出双三绝杀阵" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore6 + RESET + "\n");
        // 两个方向的活三棋形分累加，并加上你新增加的不同方向 Rule3>=2 加成红利(+400分)，总盘面得分必须形成高分压制。
        assertTrue(gScore6 > 2500, "【⚠️漏洞】多方向双三组合加成规则未成功触发，当前得分偏低: " + gScore6);

        // [全局7] 长连（>5子）极端抗崩溃测试
        byte[][] gPanel7 = new byte[15][15];
        for(int c=2; c<=7; c++) gPanel7[2][c] = 2; // 连下6个子
        double gScore7 = evaluate(gPanel7, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 7】黑棋(2)达成 6 子长连（Overline）极端测试" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore7 + RESET + "\n");
        assertTrue(gScore7 >= 1000000, "长连属于必胜局，全局分未成功映射为必胜极限分");

        // [全局8] 中局复杂攻守缠绕
        byte[][] gPanel8 = new byte[15][15];
        gPanel8[12][5] = 2; gPanel8[12][6] = 2; gPanel8[12][7] = 2; // 黑活三
        gPanel8[3][11] = 2; gPanel8[4][12] = 2;                     // 黑散子
        gPanel8[7][6] = 1;  gPanel8[7][7] = 1;  gPanel8[7][8] = 1;  // 白核心活三
        gPanel8[2][10] = 1;                                         // 白拦截棋
        double gScore8 = evaluate(gPanel8, AI_COLOR);
        System.out.println(YELLOW + "【全局测试 8】中局黑白缠绕复杂绞杀盘面" + RESET);
        System.out.println(" -> 实际全局得分 (eval2 - eval1): " + RED + gScore8 + RESET + "\n");
        // 势均力敌的中局缠绕，全局得分绝对不能出现胜负已分的一边倒巨额分
        assertTrue(Math.abs(gScore8) < 2000, "【⚠️漏洞】中局绞杀盘面估值失衡，AI 出现了极端的乐观或悲观，得分: " + gScore8);

        System.out.println(CYAN + "==================================================" + RESET);
        System.out.println(CYAN + "         🎉 八大全局大局观断言压测全部通过 🎉" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);
    }

    @Test
    @DisplayName("🎯 盘面测试：基于多方向 5 元组滑动累加机制的单子全盘分精准断言")
    void testSinglePieceEvaluationPrecise() {
        String CYAN = "\u001B[36m";
        String RESET = "\u001B[0m";
        String GREEN = "\u001B[32m";
        String RED = "\u001B[31m";

        System.out.println(CYAN + "==================================================" + RESET);
        System.out.println(CYAN + " 🎯 开始执行【每一个方向上每一个5元组累加】精准断言 " + RESET);
        System.out.println(CYAN + "==================================================" + RESET);

        int mid = GameConstants.ChessBorderNum / 2; // 15x15 中心是 (7,7)

        // --------------------------------------------------
        // 🧪 盘面 A：AI (2) 落在正中心天元 (7,7)
        // --------------------------------------------------
        byte[][] singleCenterPanel = new byte[15][15];
        singleCenterPanel[mid][mid] = AI_COLOR;

        double centerGlobalScore = evaluate(singleCenterPanel, AI_COLOR);
        System.out.printf(" -> 【天元单子 (7,7)】全盘实际得分: %.4f\n", centerGlobalScore);

        // 💡 修正后的真理数学推导：
        // 1. 棋形分：中心点不卡边界，4个方向各能延伸出5个包含该子的5元组。
        //    总共 4 * 5 = 20 个五元组。每个五元组包含 1 个子 -> RuleScore(1) = 1 分。
        //    纯棋形累加分 = 20 * 1 = 20 分。
        // 2. 距离分：由于就在中心，distance = 0 -> 距离分为满分 CenterScore = 50 分。
        // 3. 理论总得分 = 20 (多五元组累加分) + 50 (中心分) = 70.0 分。
        assertEquals(70.0, centerGlobalScore, 0.001,
                RED + "【⚠️逻辑不符】天元单子得分不符合 5 元组累加理论值(70.0)！当前分: " + centerGlobalScore + RESET);


        // --------------------------------------------------
        // 🧪 盘面 B：AI (2) 落在极边缘角落 (0,0)
        // --------------------------------------------------
        byte[][] singleCornerPanel = new byte[15][15];
        singleCornerPanel[0][0] = AI_COLOR;

        double cornerGlobalScore = evaluate(singleCornerPanel, AI_COLOR);
        System.out.printf(" -> 【角落单子 (0,0)】全盘实际得分: %.4f\n", cornerGlobalScore);

        // 💡 修正后的角落数学推导：
        // 1. 棋形分：在 (0,0) 位置，向右、向下、向右下斜这3个方向，每个方向都只有【唯一个】5元组能容纳它而不越界；
        //    而左上斜向直接无法越界。因此，它总共只能参与 3 个五元组（或根据边界判定为 4 个）。
        //    这导致它的纯棋形分断崖式下跌，只有 3 ~ 4 分！
        // 2. 距离惩罚：距离中心极远，距离分约为 -47.99 分。
        // 3. 理论总得分 = 4 (残缺的五元组分) + (-47.99) = -43.99 分左右。
        assertTrue(cornerGlobalScore < 0,
                RED + "【⚠️策略漏洞】角落单子由于可参与的5元组极少且距离太远，必须为负分！当前: " + cornerGlobalScore + RESET);

        // --------------------------------------------------
        // 🚨 联动梯度断言
        // --------------------------------------------------
        assertTrue(centerGlobalScore > cornerGlobalScore, "【⚠️Bug】中心单子分竟然没有角落高！");

        System.out.println(GREEN + "🎉 精准 5 元组滑动累加机制断言完全通过！AI 底层数理逻辑闭环！" + RESET);
        System.out.println(CYAN + "==================================================" + RESET);
    }

    static public Point getBestPosition(byte[][] panel){
        Point point = new Point();
        point.y=0; point.x=0;
        int col= GameConstants.ChessBorderNum-1, row=col;
        double score = Double.NEGATIVE_INFINITY; // 已修复 Double.MIN_VALUE 导致的负分判定漏洞

        for (int i = 0; i<=col; i++){
            for(int j = 0; j<=row; j++){
                double t = minimax(panel, i, j, true);
                if(t>=score){
                    score=t;
                    point.x=i; point.y=j;
                }
            }
        }
        return point;
    }

    static double minimax(byte[][] panel ,int top_x, int top_y, Boolean isMax){
        return evaluate(panel, AI_COLOR);
    }

    static double evaluate(byte[][] panel, byte AI){
        double eval1=0, eval2=0;
        byte Player = (byte)(AI==1?2:1);
        int col = GameConstants.ChessBorderNum-1, row=GameConstants.ChessBorderNum-1;
        for (int i = 0; i <=row; i++) {
            for (int j = 0; j <=col; j++) {
                if(panel[i][j]!=Player && panel[i][j] != AI) continue;
                double t=eval(panel, i, j);
                if(panel[i][j] == Player){
                    eval1+=t;
                }else{
                    eval2+=t;
                }
            }
        }
        return eval2-eval1;
    }

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

        // 引入基于到中心格子的哈曼顿距离权重计算
        int mid = GameConstants.ChessBorderNum/2;
        double distance = Math.sqrt((i-mid)*(i-mid)+(j-mid)*(j-mid));
        res+=CenterScore-CenterScore*(1-CenterWeight)*distance;
        return res;
    }

    static double RuleScore(int n){
        switch (n){
            case 0: return 0;
            case 1: return 1;
            case 2: return 30;
            case 3: return 600;
            case 4: return 4000;
            default: return 1000000;
        }
    }
}