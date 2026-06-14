/*
package test;

import main.java.logic.GameSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import main.java.utils.GameConstants;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestCheckWin {
    private GameSystem system;
    private byte[][] board;

    @BeforeEach
    void setUp() {
        system = new GameSystem(null, null);
        system.getGameState().initialize(GameConstants.GameMode.VS_PLAYER, (byte) 1);
        board = system.getGameState().getPanel();
    }

    @Test
    @DisplayName("横向五连胜")
    void horizontalFiveWins() {
        for (int y = 3; y <= 7; y++) {
            board[6][y] = 1;
        }

        List<Point> line = system.checkWin(6, 5);
        assertNotNull(line);
        assertEquals(5, line.size());
    }

    @Test
    @DisplayName("纵向五连胜")
    void verticalFiveWins() {
        for (int x = 2; x <= 6; x++) {
            board[x][9] = 2;
        }

        List<Point> line = system.checkWin(4, 9);
        assertNotNull(line);
        assertEquals(5, line.size());
    }

    @Test
    @DisplayName("左上到右下斜向五连胜")
    void diagonalDownWins() {
        for (int i = 0; i < 5; i++) {
            board[3 + i][4 + i] = 1;
        }

        assertNotNull(system.checkWin(5, 6));
    }

    @Test
    @DisplayName("左下到右上斜向五连胜")
    void diagonalUpWins() {
        for (int i = 0; i < 5; i++) {
            board[8 - i][2 + i] = 2;
        }

        assertNotNull(system.checkWin(6, 4));
    }

    @Test
    @DisplayName("不足五个不胜")
    void fourDoesNotWin() {
        for (int y = 4; y <= 7; y++) {
            board[7][y] = 1;
        }

        assertNull(system.checkWin(7, 5));
    }

    @Test
    @DisplayName("六连也算胜利")
    void sixAlsoWins() {
        for (int y = 1; y <= 6; y++) {
            board[10][y] = 1;
        }

        List<Point> line = system.checkWin(10, 4);
        assertNotNull(line);
        assertTrue(line.size() >= 5);
    }

    @Test
    @DisplayName("边界位置五连胜")
    void edgeWin() {
        for (int x = 0; x < 5; x++) {
            board[x][0] = 2;
        }

        assertNotNull(system.checkWin(0, 0));
    }
}
*/
