package ai.Minimax;

import utils.GameConstants;

public class EvaluatorRule {
    public static final double[][] centerDistanceWeightTable = new double[GameConstants.ChessBorderNum][GameConstants.ChessBorderNum];
    static double CenterWeight = 0.80;
    static double CenterScore = 40;

    static {
        int mid = GameConstants.ChessBorderNum / 2;
        for (int r = 0; r < GameConstants.ChessBorderNum; r++) {
            for (int c = 0; c < GameConstants.ChessBorderNum; c++) {
                double distance = Math.sqrt((r - mid) * (r - mid) + (c - mid) * (c - mid));
                centerDistanceWeightTable[r][c] = CenterScore - CenterScore * (1 - CenterWeight) * distance;
            }
        }
    }

    // 棋盘全盘局势评估
    public double evaluate(byte[][] panel, byte aiColor, byte playerColor, int[][] direction) {
        double eval1 = 0, eval2 = 0;
        int row = GameConstants.ChessBorderNum - 1;
        for (int i = 0; i <= row; i++) {
            for (int j = 0; j <= row; j++) {
                byte p = panel[i][j];
                if (p != playerColor && p != aiColor) continue;
                double t = eval(panel, i, j, direction);
                if (p == playerColor) eval1 += t;
                else eval2 += t;
            }
        }
        return eval2 - eval1;
    }

    // 局部落子打分
    public double eval(byte[][] panel, int x, int y, int[][] direction) {
        byte flag = panel[x][y];
        double score = 0;
        int rule3 = 0;
        int rule4 = 0;

        for (int d = 0; d < 4; d++) {
            int dx = direction[d][0];
            int dy = direction[d][1];

            int count = 1;
            int leftOpen = 0;
            int rightOpen = 0;

            int cx = x + dx, cy = y + dy;
            while (isValid(cx, cy) && panel[cx][cy] == flag) {
                count++;
                cx += dx;
                cy += dy;
            }
            if (isValid(cx, cy) && panel[cx][cy] == 0) rightOpen = 1;

            cx = x - dx;
            cy = y - dy;
            while (isValid(cx, cy) && panel[cx][cy] == flag) {
                count++;
                cx -= dx;
                cy -= dy;
            }
            if (isValid(cx, cy) && panel[cx][cy] == 0) leftOpen = 1;

            int patternScore = getPatternScore(count, leftOpen, rightOpen);
            score += patternScore;

            boolean isLive = (leftOpen == 1 && rightOpen == 1);
            if (count == 4 && isLive) rule4++;
            else if (count == 3 && isLive) rule3++;

            if (count == 4 && (leftOpen + rightOpen == 1)) {
                score += 28000;
            }
        }

        if (rule4 >= 2) {
            score += 85000;
        } else if (rule4 == 1 && rule3 >= 1) {
            score += 32000;
        } else if (rule3 >= 2) {
            score += 18000;
        }

        score += centerDistanceWeightTable[x][y] * 1.1;
        return score;
    }

    public boolean isValid(int x, int y) {
        return x >= 0 && x < GameConstants.ChessBorderNum && y >= 0 && y < GameConstants.ChessBorderNum;
    }

    public int getPatternScore(int count, int leftOpen, int rightOpen) {
        boolean live = (leftOpen == 1 && rightOpen == 1);
        int openSides = leftOpen + rightOpen;

        if (count >= 5) return 1200000;

        switch (count) {
            case 4: return live ? 65000 : 28000;
            case 3: return live ? 7200 : 800;
            case 2: return live ? 280 : 40;
            case 1: return openSides > 0 ? 12 : 0;
            default: return 0;
        }
    }

    public int quickSniffByColor(byte[][] panel, int x, int y, byte color, int[][] direction) {
        int maxSeq = 0;
        for (int d = 0; d < 4; d++) {
            int dx = direction[d][0], dy = direction[d][1];
            int cnt = 1;

            for (int j = 1; j <= 4; j++) {
                int tx = x + dx * j, ty = y + dy * j;
                if (!isValid(tx, ty)) break;
                if (panel[tx][ty] == color) cnt++;
                else break;
            }
            for (int j = 1; j <= 4; j++) {
                int tx = x - dx * j, ty = y - dy * j;
                if (!isValid(tx, ty)) break;
                if (panel[tx][ty] == color) cnt++;
                else break;
            }
            maxSeq = Math.max(maxSeq, cnt);
            if (maxSeq >= 5) return 5;
        }
        return maxSeq;
    }
}