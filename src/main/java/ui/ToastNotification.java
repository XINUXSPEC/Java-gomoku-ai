package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ToastNotification {
    private final String message;
    private float alpha = 0.0f;

    // 🎯 解决死锁关键：去掉 final，允许分步优雅注入
    private Timer fadeInTimer;
    private Timer fadeOutTimer;
    private final Component parentComponent;

    public ToastNotification(Component parent, String message) {
        this.parentComponent = parent;
        this.message = message;

        // 🎯 动画逻辑 1：渐现 (Fade In)
        fadeInTimer = new Timer(15, e -> {
            alpha += 0.1f;
            if (alpha >= 0.9f) {
                alpha = 0.9f;
                fadeInTimer.stop();

                Timer delayTimer = new Timer(800, ev -> fadeOutTimer.start());
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
            parentComponent.repaint();
        });

        // 🎯 动画逻辑 2：渐现 (Fade Out)
        fadeOutTimer = new Timer(25, e -> {
            alpha -= 0.05f;
            if (alpha <= 0.0f) {
                alpha = 0.0f;
                fadeOutTimer.stop();
            }
            parentComponent.repaint();
        });
    }

    public void showNotification() {
        if (fadeInTimer.isRunning() || fadeOutTimer.isRunning()) {
            return;
        }
        alpha = 0.0f;
        fadeInTimer.start();
    }

    public boolean isActive() {
        return alpha > 0.0f || (fadeInTimer != null && fadeInTimer.isRunning()) || (fadeOutTimer != null && fadeOutTimer.isRunning());
    }

    public void render(Graphics2D g2d, int containerWidth, int containerHeight) {
        if (alpha <= 0.01f) return;

        Graphics2D g2 = (Graphics2D) g2d.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int toastWidth = 260;
        int toastHeight = 50;
        int x = (containerWidth - toastWidth) / 2;
        int y = (containerHeight - containerHeight) / 2; // 居中修正
        x = (containerWidth - toastWidth) / 2;
        y = (containerHeight - toastHeight) / 2;

        g2.setColor(new Color(30, 30, 30));
        g2.fill(new RoundRectangle2D.Float(x, y, toastWidth, toastHeight, 20, 20));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        FontMetrics metrics = g2.getFontMetrics();
        int stringWidth = metrics.stringWidth(message);
        int stringHeight = metrics.getAscent();

        int textX = x + (toastWidth - stringWidth) / 2;
        int textY = y + (toastHeight - metrics.getHeight()) / 2 + stringHeight;

        g2.drawString(message, textX, textY);
        g2.dispose();
    }
}