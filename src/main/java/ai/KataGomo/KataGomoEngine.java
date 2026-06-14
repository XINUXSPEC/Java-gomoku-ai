package ai.KataGomo;

import ai.AI_Engine;
import java.awt.Point;
import java.io.*;

public class KataGomoEngine implements AI_Engine {

    // 引擎进程核心组件
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    
    private byte aiColor;             // AI当前的棋子颜色
    private final int boardSize = 15; // 固定15x15棋盘

    // 路径配置（请根据实际存放位置修改）
    private final String exePath;
    private final String configPath;
    private final String modelPath;

    /**
     * 构造函数：初始化对局进程
     */
    public KataGomoEngine() {
        String currentDir = System.getProperty("user.dir");
        // 自动兼容 Windows 路径分隔符，彻底抹平盘符和虚拟路径的问题
        this.exePath = currentDir + File.separator + "engine" + File.separator + "gom15x_trt.exe";
        this.configPath = currentDir + File.separator + "engine" + File.separator + "gtp.cfg";
        this.modelPath = currentDir + File.separator + "engine" + File.separator + "zhizi_renju28b_s1600.bin.gz";
    }

    // 加载模型
    public void LoadMode(){
        try {
            System.out.println(exePath);
            System.out.println(configPath);
            System.out.println(modelPath);
            ProcessBuilder pb = new ProcessBuilder(
                    exePath, "gtp",
                    "-config", configPath,
                    "-model", modelPath
            );
            pb.redirectErrorStream(true);
            this.process = pb.start();
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            System.out.println("[KataGomoEngine] 后台进程已挂载。");

            // 3. 强锁非禁手自由五子棋规则
            sendCommand("boardsize " + boardSize);
            sendCommand("clear_board");
            System.out.println("[KataGomoEngine] 非禁手规则环境初始化成功。");
        } catch (IOException e) {
            System.err.println("[KataGomoEngine] 无法拉起后台进程！"+e);
            e.printStackTrace();
        }
    }

    /**
     * 进程间标准交互：发送命令并死等返回
     */
    private String sendCommand(String cmd) throws IOException {
        try {
            writer.write(cmd + "\n");
            writer.flush(); // 强制刷新进管道

            StringBuilder response = new StringBuilder();
            String line;

            // 当读到空行时代表该条 GTP 指令的响应接收完毕
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    break;
                }
                response.append(line).append("\n");
            }
            return response.toString().trim();
        } catch (IOException e) {
            throw e;
        }
    }

    //坐标翻译（GTP协议强行跳过字母 'I'
    private String toGtpCoordinate(int x, int y) {
        int row = x;
        int col = y;

        char[] colLetters = "ABCDEFGHJKLMNOPQRST".toCharArray(); // 跳过字母 'I'
        char letter = colLetters[col];

        int gtpRow = boardSize - row; // 纵坐标上下翻转
        return "" + letter + gtpRow;
    }

    /**
     * 将 GTP 字符串（如 "J9"）还原为 Java 的 Point(x=行, y=列)
     */
    private Point toJavaCoordinate(String gtpCoord) {
        char letter = gtpCoord.toUpperCase().charAt(0);
        int gtpRow = Integer.parseInt(gtpCoord.substring(1));

        String letters = "ABCDEFGHJKLMNOPQRST";
        int col = letters.indexOf(letter); // 对应你的 y
        int row = boardSize - gtpRow;       // 对应你的 x

        // 🎯 核心修正：返回给你的 Point，x 填行(row)，y 填列(col)
        return new Point(row, col);
    }

    /**
     * 当你开始游戏或重新开始游戏时，外部调用 Init
     * 内部安全物理销毁旧进程，打开新进程并强锁非禁手规则
     */
    @Override
    public void Init(byte color) {
        this.aiColor = color;
        if (process != null && process.isAlive()) {
            try {
                sendCommand("clear_board");
                System.out.println("[KataGomoEngine] 引擎复用，仅清空棋盘");
            } catch (IOException e) {
                System.err.println("[KataGomoEngine] clear_board失败，重启进程");
            }
        } else {
            System.err.println("[KataGomoEngine] 进程不存在,重启游戏");
        }
    }

    @Override
    public Point findBestMove(byte[][] board) {
        return findBestMove(board, null);
    }

    /**
     * 纯粹的计算接口。外部开启新线程调用此方法时，会同步阻塞等待后台进程返回
     */
    @Override
    public Point findBestMove(byte[][] board, Point lastMove) {
        try {
            // 1. 同步玩家最后一手给进程
            if (lastMove != null) {
                String oppMoveGtp = toGtpCoordinate(lastMove.x, lastMove.y);
                String oppColorStr = (aiColor == 1) ? "white" : "black";
                sendCommand("play " + oppColorStr + " " + oppMoveGtp);
            }

            // 2. 向进程索要 AI 走法
            String aiColorStr = (aiColor == 1) ? "black" : "white";
            String response = sendCommand("genmove " + aiColorStr);

            // 3. 解析并转换 GTP 返回的坐标
            if (response.startsWith("=")) {
                // 成功拿到坐标：例如 "= J8"
                String aiMoveGtp = response.replace("=", "").trim();

                // 防御特例：如果 AI 选择认输（虽然配置里关了，但在极限必输盘面时仍有可能触发）
                if (aiMoveGtp.equalsIgnoreCase("resign")) {
                    throw new IOException("[KataGomoEngine] AI 判定当前局势已毫无胜率，底层触发强制认输(Resign)！");
                }

                // 防御特例：如果 AI 突然选择虚着停一手（Pass）
                if (aiMoveGtp.equalsIgnoreCase("pass")) {
                    throw new IOException("[KataGomoEngine] AI 选择了停一手(Pass)，盘面可能已无处可落。");
                }

                return toJavaCoordinate(aiMoveGtp); // 完美通关，返回 Point(x=行, y=列)
            }
            if (response.startsWith("?")) {
                // 引擎明确表示报错：例如 "? Unknown command"
                throw new IOException("[KataGomoEngine] 引擎拒绝了 genmove 指令！底层报错: " + response);
            }

        } catch (IOException e) {
            System.err.println("[KataGomoEngine] 管道通信中途发生异常。");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 销毁AI引擎：强制杀掉后台进程
     * 快速销毁：跳过优雅 quit，直接 force kill
     */
    @Override
    public void dispose() {
        System.out.println("[KataGomoEngine] 正在销毁引擎...");
        forceKillProcess();
        System.out.println("[KataGomoEngine] 引擎已销毁");
    }

    /**
     * 快速强制杀掉 KataGomo 进程
     */
    private void forceKillProcess() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) { /* 静默 */ }
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (IOException e) { /* 静默 */ }

        try {
            if (process != null) {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                process = null;
            }
        } catch (Exception e) { /* 静默 */ }
    }
}