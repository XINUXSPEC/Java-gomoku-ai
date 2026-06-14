package AIArena;

import ai.AI_Engine;
import ai.KataGomo.KataGomoEngine;
import ai.Minimax.MiniMaxEngine;

public class MainTest {
    public static void main(String[] args) {

        // -------------------------------------------------------------
        // 🎛️ 核心矩阵调参：定义你想测试的 MiniMax 深度梯度
        // -------------------------------------------------------------
        int[] testDepths = {6,8,10}; // 自动依次测试 2层、4层、6层 (如果算力够，可以加上 8)

        long miniMaxTimeLimit = 5000;  // MiniMax 每次卡秒限时 (毫秒)
        int totalGamesPerTournament = 10; // 每个层数分别对战多少局
        boolean enableColorRotation = true; // 开启先后手轮转

        System.out.println("====== 🏁 开始执行全自动 [多层次] AI 算力压力测试矩阵 🏁 ======");
        System.out.printf("计划测试深度梯度: %s | 每组对局数: %d 局\n",
                java.util.Arrays.toString(testDepths), totalGamesPerTournament);
        System.out.println("===============================================================");

        for (int depth : testDepths) {
            System.out.println("\n\n###############################################################");
            System.out.printf("🚀 [矩阵激活] 当前正在执行：MiniMaxEngine (深度: %d 层) vs KataGomoEngine\n", depth);
            System.out.println("###############################################################");

            // 1. 每个循环动态创建一个该层数的传统引擎
            AI_Engine miniMaxEngine = new MiniMaxEngine(depth, miniMaxTimeLimit);

            // 2. 🎯 底层死穴：必须在每个循环内部实例化独立的大模型引擎
            System.out.println("[系统准备] 正在热身神经网络 KataGomoEngine (RTX 4070)...");
            AI_Engine kataGomoEngine = new KataGomoEngine();
            // 注意：Init 会在 AIArena 内部每局开始前自动调用，这里不需要手动 Init

            // 3. 构建对战擂台
            AIArena arena = new AIArena(kataGomoEngine, miniMaxEngine);

            // 4. 开启比赛
            try {
                arena.startTournament(totalGamesPerTournament, enableColorRotation);
            } finally {
                // 5. 🎯 终极闭环：比赛完后，必须物理强制销毁大模型进程，释放显存！
                // 如果你的 KataGomoEngine 有 close() 或 quit() 方法，请在这里调用。
                // 否则下一轮循环拉起新引擎时，旧的 C++ 进程还在后台挂着，显存很快就爆了。
                if (kataGomoEngine instanceof KataGomoEngine) {
                    // ((KataGomoEngine) kataGomoEngine).close(); // 视你底层销毁进程的方法而定
                }
            }
        }

        System.out.println("\n\n===============================================================");
        System.out.println("🎉 🎉 所有层次的算力矩阵测试已全部全自动执行完毕！");
        System.out.println("👉 请前往项目根目录查看各个深度的 arena_report_*.html 网页曲线吧！");
        System.out.println("===============================================================");
    }
}