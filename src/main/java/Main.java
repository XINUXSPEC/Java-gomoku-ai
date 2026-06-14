import   Controller.GameController;
import   ui.StartGameGUI;
import   utils.BoardUtils;
import   javax.swing.*;
import   java.awt.*;

public class Main {
    public static void main(String[] args) {
        initTheme();
        BoardUtils.InitBoardUtils();
        SwingUtilities.invokeLater(() -> {
            StartGameGUI gui = new StartGameGUI();
            gui.setVisible(true);
            GameController controller = new GameController(gui);
            gui.AddButtonListener(controller);
        });
    }

    private static void initTheme() {
        UIManager.put("OptionPane.background", new Color(45, 45, 52));
        UIManager.put("Panel.background", new Color(45, 45, 52));
        UIManager.put("Button.background", new Color(70, 130, 180));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
    }
}
