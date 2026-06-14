package utils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SoundManager {
    private static final float SAMPLE_RATE = 44100f;
    private static final ThreadPoolExecutor soundExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    private SoundManager() {
    }

    /** 落子音效：棋子落在木质棋盘上的短促敲击声 */
    public static void playMoveSound() {
        soundExecutor.execute(SoundManager::playStonePlaceSound);
    }

    private static void playStonePlaceSound() {
        try {
            int durationMs = 120; // 适当延长一点，声音更有质感
            int sampleCount = (int) (SAMPLE_RATE * durationMs / 1000);
            byte[] buffer = new byte[sampleCount * 2];
            java.util.Random random = new java.util.Random();

            for (int i = 0; i < sampleCount; i++) {
                double t = i / SAMPLE_RATE;

                // 优化包络：使用更缓慢的衰减，让声音不那么生硬
                double envelope = Math.exp(-t * 25);

                // 木质共鸣：增加一个深沉的低频分量(80Hz)
                double thud = Math.sin(2 * Math.PI * 80 * t) * 0.4
                        + Math.sin(2 * Math.PI * 160 * t) * 0.4
                        + Math.sin(2 * Math.PI * 320 * t) * 0.2;

                // 敲击感：使用更短促的随机白噪声，并随时间快速衰减
                double click = 0;
                if (t < 0.008) {
                    click = (random.nextDouble() * 2 - 1) * 0.95 * (1 - t * 100);
                }

                // 增大整体音量系数至 0.95
                double sample = (thud + click) * envelope * 0.95;
                short pcm = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample * 32767));

                buffer[i * 2] = (byte) (pcm & 0xFF);
                buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {
        }
    }
}
