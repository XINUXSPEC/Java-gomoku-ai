package State;

import   utils.GameConstants;

public class GameState {
    private byte[][] panel;
    private byte currentTurn;
    private byte[] playerColors;
    private GameConstants.GameMode gameMode;
    private byte firstMovePlayer=1;
    private boolean Ruing = false;

    public GameState() {
        int size = GameConstants.ChessBorderNum;
        panel = new byte[size][size];
        playerColors = new byte[]{0, 1, 2};
        currentTurn = 1;
        gameMode = GameConstants.GameMode.VS_PLAYER;
    }

    public void initialize(GameConstants.GameMode mode, byte firstMovePlayer) {
        this.gameMode = mode;
        this.firstMovePlayer = firstMovePlayer;
        this.currentTurn = firstMovePlayer;
        this.Ruing = true;

        playerColors[firstMovePlayer] = 1;
        playerColors[3 - firstMovePlayer] = 2;

        cleanPanel();
    }

    private void cleanPanel() {
        for (int i = 0; i < GameConstants.ChessBorderNum; i++) {
            for (int j = 0; j < GameConstants.ChessBorderNum; j++) {
                panel[i][j] = 0;
            }
        }
    }

    public byte[][] getPanel() {
        return panel;
    }

    public void setPanel(byte[][] panel) {
        this.panel = panel;
    }

    public byte getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(byte currentTurn) {
        this.currentTurn = currentTurn;
    }

    public byte[] getPlayerColors() {
        return playerColors;
    }

    public byte getPlayerColor(int player) {
        return playerColors[player];
    }

    public byte getPlayerByColor(byte color) {
        if (playerColors[1] == color) {
            return 1;
        } else if (playerColors[2] == color) {
            return 2;
        }
        return 0;
    }

    public GameConstants.GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameConstants.GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void switchTurn() {
        currentTurn = (byte) (3 - currentTurn);
    }

    public byte getCurrentPlayerColor() {
        return playerColors[currentTurn];
    }

    public boolean isEmpty(int x, int y) {
        return panel[x][y] == 0;
    }

    public void setPiece(int x, int y, byte color) {
        panel[x][y] = color;
    }

    public byte getPiece(int x, int y) {
        return panel[x][y];
    }

    public byte getFirstMovePlayer() {
        return firstMovePlayer;
    }

    public void setFirstMovePlayer(byte firstMovePlayer) {
        this.firstMovePlayer = firstMovePlayer;
    }

    public boolean isRuing() {
        return Ruing;
    }

    public void setRuing(boolean ruing) {
        Ruing = ruing;
    }
}
