package ai;

import java.awt.*;

public interface AI_Engine {
    Point findBestMove(byte[][] board); //返回计算得出的落子位置
    Point findBestMove(byte[][] board, Point lastMove); //重载：包含上一手位置
    void Init(byte color);    //新一局游戏或重新开始时初始化（包含清空棋盘）
    default void dispose(){}
}