package com.samsara.client.debug;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Công cụ chụp ảnh tự động cho phiên phát triển (chỉ chạy trong môi trường dev, và chỉ khi có file
 * {@code run/samsara_shots.txt}). Ở màn hình chính nó tự vào world ghi ở dòng đầu, rồi chạy từng lệnh:
 * <pre>
 *   world New World (2)     tên world trong run/saves (dòng đầu)
 *   wait 100                chờ 100 tick
 *   cmd samsara test ...    gửi lệnh (không có dấu /)
 *   look -90 20             đặt hướng nhìn (yaw, pitch)
 *   hud off|on              ẩn / hiện giao diện
 *   shot ten_anh            lưu run/screenshots/ten_anh.png (có shader)
 *   quit                    thoát game
 * </pre>
 * Màn cảnh báo "Worlds using Experimental Settings" được tự bấm "I know what I'm doing!".
 * Người chơi thường không bao giờ gặp: bản phát hành không phải môi trường dev.
 */
public final class ScriptedShots {
    private ScriptedShots() {}

    private static List<String> lines = null;
    private static int index = 0;
    private static int waitTicks = 0;
    private static boolean joining = false;

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        Path file = FabricLoader.getInstance().getGameDir().resolve("samsara_shots.txt");
        if (!Files.exists(file)) return;
        try {
            lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(ScriptedShots::tick);
    }

    private static void tick(MinecraftClient client) {
        if (lines == null || index >= lines.size()) return;
        client.options.pauseOnLostFocus = false;

        if (client.player == null || client.world == null) {
            // World dev dùng cài đặt thử nghiệm: tự bấm "I know what I'm doing!" (vào không tạo bản sao lưu)
            if (client.currentScreen != null) {
                for (net.minecraft.client.gui.Element e : client.currentScreen.children()) {
                    if (e instanceof net.minecraft.client.gui.widget.ButtonWidget b
                            && b.getMessage().getContent() instanceof net.minecraft.text.TranslatableTextContent t
                            && t.getKey().equals("selectWorld.backupJoinSkipButton")) {
                        b.onPress();
                        return;
                    }
                }
            }
            // Vào world ở dòng đầu khi đang ở màn hình chính
            if (!joining && client.currentScreen instanceof TitleScreen && lines.get(0).startsWith("world ")) {
                joining = true;
                client.createIntegratedServerLoader().start(client.currentScreen, lines.get(0).substring(6).trim());
            }
            return;
        }
        if (index == 0 && lines.get(0).startsWith("world ")) {
            index = 1;
            waitTicks = 10;    // chạy lệnh đầu thật sớm (vd. rời khỏi chỗ đứng cũ trước khi có gì tự kích hoạt)
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (client.currentScreen != null) client.setScreen(null);

        String line = lines.get(index++).trim();
        if (line.isEmpty() || line.startsWith("#")) return;
        String[] p = line.split(" ", 2);
        String arg = p.length > 1 ? p[1].trim() : "";
        switch (p[0]) {
            case "wait" -> waitTicks = Integer.parseInt(arg);
            case "cmd" -> client.player.networkHandler.sendChatCommand(arg);
            case "look" -> {
                String[] a = arg.split(" ");
                client.player.setYaw(Float.parseFloat(a[0]));
                client.player.setHeadYaw(Float.parseFloat(a[0]));
                client.player.setPitch(Float.parseFloat(a[1]));
            }
            case "hud" -> client.options.hudHidden = arg.equals("off");
            case "shot" -> ScreenshotRecorder.saveScreenshot(new File(client.runDirectory.getAbsolutePath()),
                    arg + ".png", client.getFramebuffer(), text -> {});
            case "quit" -> client.scheduleStop();
            default -> { }
        }
    }
}
