package com.samsara.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.samsara.system.celestial.SolarFractureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.util.function.Consumer;

/**
 * "Thứ ghi hình thấy sự thật": ảnh chụp màn hình (F2) ghi được mặt trời thật (hố đen theo stage) dù mắt vẫn thấy
 * mặt trời vanilla.
 *
 * Cách làm: bấm F2 (stage ≥1, không đang ngắm máy quay) thì chưa chụp ngay. Chép khung hình đang có trên màn hình
 * vào {@link #lastFrame}, rồi khung kế tiếp được vẽ với mặt trời hố đen ({@link #isCapturing()}, đọc ở
 * {@code WorldRendererMixin}). Trước khi khung đó lên màn hình, chụp nó vào file, rồi đưa {@link #lastFrame} lên màn
 * hình thay cho nó ({@code MinecraftClientScreenshotMixin}). Người chơi chỉ thấy khung cũ lặp lại một lần.
 * Lệnh test: /samsara test screenshot_sun.
 */
public final class ScreenshotSunClient {
    private ScreenshotSunClient() {}

    private static SimpleFramebuffer lastFrame;
    private static boolean capturing;
    private static File pendingDir;
    private static Consumer<Text> pendingCallback;

    /** Lệnh test: server bảo client chụp màn hình như bấm F2. */
    public static void register() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                SolarFractureManager.SCREENSHOT_SUN_TEST, (client, handler, buf, sender) -> client.execute(() ->
                        requestScreenshot(client.runDirectory, client.getFramebuffer(),
                                msg -> client.execute(() -> client.inGameHud.getChatHud().addMessage(msg)))));
    }

    /** Khung hình đang vẽ là khung để chụp: vẽ mặt trời thật. */
    public static boolean isCapturing() {
        return capturing;
    }

    /** Thay cho {@code ScreenshotRecorder.saveScreenshot} khi bấm F2 (gọi trên render thread). */
    public static void requestScreenshot(File dir, Framebuffer fb, Consumer<Text> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (capturing || client.world == null || SolarFractureManager.getSunTexture() == null
                || CamcorderClientManager.isFullyAimed()) {
            ScreenshotRecorder.saveScreenshot(dir, fb, callback);
            return;
        }
        copyToLastFrame(fb);
        pendingDir = dir;
        pendingCallback = callback;
        capturing = true;
    }

    /** Thay cho {@code Framebuffer.draw(w, h)} trong {@code MinecraftClient.render}. */
    public static void drawToScreen(Framebuffer fb, int width, int height) {
        if (!capturing) {
            fb.draw(width, height);
            return;
        }
        capturing = false;
        ScreenshotRecorder.saveScreenshot(pendingDir, fb, pendingCallback);
        pendingDir = null;
        pendingCallback = null;
        boolean sameSize = lastFrame != null && lastFrame.textureWidth == fb.textureWidth && lastFrame.textureHeight == fb.textureHeight;
        if (sameSize) lastFrame.draw(width, height);
        else fb.draw(width, height);
    }

    private static void copyToLastFrame(Framebuffer fb) {
        int w = fb.textureWidth, h = fb.textureHeight;
        if (lastFrame == null) {
            lastFrame = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
        } else if (lastFrame.textureWidth != w || lastFrame.textureHeight != h) {
            lastFrame.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.fbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, lastFrame.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}
