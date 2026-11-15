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
 *   aim on|off              ngắm / thôi ngắm máy quay (phải đang cầm máy quay)
 *   shot ten_anh            lưu run/screenshots/ten_anh.png (có shader)
 *   use                     bấm chuột phải vào khối đang nhìn
 *   slot 37 [0|1] [quick]    bấm ô trong giao diện đang mở (túi đồ, bàn chế tạo); close để đóng
 *   hotbar 2                chọn ô thanh công cụ
 *   shaders on|off          bật / tắt shaderpack (Iris)
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
        // Đóng màn hình đang mở trước mỗi lệnh, trừ giao diện túi đồ / bàn chế tạo (để lệnh slot bấm được; đóng bằng close)
        if (client.currentScreen != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>))
            client.setScreen(null);

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
            case "aim" -> com.samsara.client.CamcorderClientManager.setAiming(arg.equals("on"), client.player);
            case "angel" -> com.samsara.client.tangduoi.LoiDiNhapAngel.forceStage(Integer.parseInt(arg));
            case "shot" -> ScreenshotRecorder.saveScreenshot(new File(client.runDirectory.getAbsolutePath()),
                    arg + ".png", client.getFramebuffer(), text -> {});
            case "use" -> {
                // Bấm chuột phải vào khối đang nhìn (mở cửa, bấm giường...)
                if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit && client.interactionManager != null) {
                    client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, hit);
                }
            }
            case "attack" -> {
                // Đánh thực thể đang nhìn (vd. cừu ở tầng 4)
                if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit && client.interactionManager != null) {
                    client.interactionManager.attackEntity(client.player, hit.getEntity());
                }
            }
            case "shaders" -> {
                // Bật / tắt shaderpack qua Iris (để thử phương án dự phòng khi không có shader)
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object config = api.getMethod("getConfig").invoke(api.getMethod("getInstance").invoke(null));
                    config.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, arg.equals("on"));
                } catch (Throwable ignored) {
                }
            }
            case "slot" -> {
                // Bấm ô trong giao diện đang mở: slot <số ô> [nút 0|1] [pickup|quick]
                String[] a = arg.split(" ");
                if (client.interactionManager != null && client.player.currentScreenHandler != null) {
                    int button = a.length > 1 ? Integer.parseInt(a[1]) : 0;
                    net.minecraft.screen.slot.SlotActionType type = a.length > 2 && a[2].equals("quick")
                            ? net.minecraft.screen.slot.SlotActionType.QUICK_MOVE : net.minecraft.screen.slot.SlotActionType.PICKUP;
                    client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, Integer.parseInt(a[0]), button, type, client.player);
                }
            }
            case "close" -> {
                if (client.player.currentScreenHandler != client.player.playerScreenHandler) client.player.closeHandledScreen();
                client.setScreen(null);
            }
            case "hotbar" -> client.player.getInventory().selectedSlot = Integer.parseInt(arg);
            case "quit" -> client.scheduleStop();
            default -> { }
        }
    }
}
