package utils;

import java.awt.*;
import java.util.HashMap;

public class BoardUtils {

    private final static HashMap<Integer, Integer> IndexToCoordinate = new HashMap<>();    // 下标到坐标
    private final static HashMap<Integer, Integer> CoordinateToIndex = new HashMap<>();    // 下标到坐标

    //计算棋子的位置
    private static Point CalculatePoint(int x, int y) {
        int xx = GameConstants.MainPadding - GameConstants.ChessSize / 2, yy = xx;
        xx += x * GameConstants.ChessBoardSize;
        yy += y * GameConstants.ChessBoardSize;
        return new Point(xx, yy);
    }

    //获取当前位置在哪个下标下面(碰撞检测,x与y是坐标)
    public static Point GetPosition(int x, int y) {
        int distance_x = 111111, distance_y = 111111, X = -1, Y = -1;
        for (int i = -GameConstants.ChessSize ; i <= 0; i++) {
            if (BoardUtils.IndexToCoordinate.containsValue(x + i)) {
                if (distance_x == Math.abs(i)) return null;
                if (distance_x > Math.abs(i)) {
                    distance_x = Math.abs(i);
                    X = x + i;
                }
            }
            if (BoardUtils.IndexToCoordinate.containsValue(y + i)) {
                if (distance_y == Math.abs(i)) return null;
                if (distance_y > Math.abs(i)) {
                    distance_y = Math.abs(i);
                    Y = y + i;
                }
            }
        }
        if (X == -1 || Y == -1) return null;
        return new Point(CoordinateToIndex.get(X), CoordinateToIndex.get(Y));
    }
    // 初始化工具类
    public static void InitBoardUtils() {
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            BoardUtils.IndexToCoordinate.put(i, CalculatePoint(i, 0).x);
            BoardUtils.CoordinateToIndex.put(CalculatePoint(i, 0).x,i);
        }
    }
    // 棋子下标转换棋子坐标
    public static int IndexTurnCoordinate(int index) {
        return BoardUtils.IndexToCoordinate.get(index);
    }

    public static boolean IsLegal(int x,int y){
        return x>=0&&y>=0&&x<=18&&y<=18;
    }
}