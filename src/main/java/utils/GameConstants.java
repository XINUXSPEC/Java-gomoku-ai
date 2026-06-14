package utils;

import java.awt.*;

public class GameConstants {
    // 窗口属性与现代感扁平化设计
    public static final int WindowWidth = 840;
    public static final int WindowHeight = 960;  // 降低整体高度，比例更协调

    public static final int PanelWidth = GameConstants.WindowWidth;
    public static final int PanelHeight = 65;    // 🔥 顶部和底部瘦身（从100px降到65px），释放空间给棋盘

    public static final int ChessBorderNum = 15; // 15*15棋盘

    // 🔥 核心改动：增大格子尺寸（从42 -> 48），棋盘会显著变大！
    public static final int ChessBordWidth = 48;
    public static final int ChessBoardSize = ChessBordWidth;

    // 棋盘网格的总实际物理宽度：48 * 14 = 672px
    public static final int ChessBorderSize = ChessBoardSize * (ChessBorderNum - 1);

    // 🔥 动态居中内边距：(830 - 672) / 2 = 79px，确保棋盘完美居中且不拥挤
    public static final int MainPadding = (WindowWidth - 10 - ChessBorderSize) / 2;
    public static final int TianYuanSize = 10;       // 现代设计天元不宜过大
    public static final int ChessBias = 6;           // 缩小偏差，棋子更大更饱满
    public static final int ChessSize = ChessBoardSize - ChessBias;

    // 🔥 现代极简风格配色定义
    public static final Color COLOR_BG_DARK   = new Color(30, 30, 36);    // 极客暗黑背景
    public static final Color COLOR_CARD_NAV  = new Color(40, 40, 48);    // 导航栏/面板深灰
    public static final Color COLOR_GRID_LINE = new Color(75, 60, 45, 180); // 细腻不刺眼的木质网格线

    // 棋子高质感颜色
    public static final Color COLOR_PIECE_BLACK = new Color(25, 25, 25);
    public static final Color COLOR_PIECE_WHITE = new Color(245, 245, 245);

    // 🔥 高级半透明残影（亮色提示，绝不会看错颜色）
    public static final Color PREVIEW_BLACK = new Color(0, 0, 0, 110);
    public static final Color PREVIEW_WHITE = new Color(255, 255, 255, 150);

    public enum GameMode {
        VS_PLAYER,
        VS_AI
    }

    public enum AIEngine {
        MINIMAX("Minimax", "基于αβ剪枝算法，搜索效率高，局面评估精准"),
        ALPHA_ZERO("AlphaZero", "深度学习神经网络，棋感直观，反应迅速");

        private final String displayName;
        private final String description;

        AIEngine(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum AIDifficulty {
        EASY("简单", 2, 300),
        NORMAL("普通", 6, 700),
        HARD("困难", 10, 1200);

        private final String displayName;
        private final int searchDepth;
        private final long timeLimitMillis;

        AIDifficulty(String displayName, int searchDepth, long timeLimitMillis) {
            this.displayName = displayName;
            this.searchDepth = searchDepth;
            this.timeLimitMillis = timeLimitMillis;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
