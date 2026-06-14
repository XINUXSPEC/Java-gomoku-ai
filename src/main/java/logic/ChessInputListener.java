package logic;

import   Controller.GameController;
import   utils.BoardUtils;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * 棋盘输入监听器
 * 
 * 职责：
 * 1. 监听鼠标点击和移动事件
 * 2. 转换屏幕坐标为棋盘坐标
 * 3. 将事件转发给 GameController 处理
 */
public class ChessInputListener implements MouseListener, MouseMotionListener {
    private final GameController gameController;

    public ChessInputListener(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point point = BoardUtils.GetPosition(e.getX(), e.getY());
        if (point == null) {
            return;
        }
        gameController.handleBoardClick(point.x, point.y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point point = BoardUtils.GetPosition(e.getX(), e.getY());
        if (point == null) {
            gameController.handleMouseExit();
            return;
        }
        gameController.handleMouseMove(point.x, point.y);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        gameController.handleMouseExit();
    }

    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
}
