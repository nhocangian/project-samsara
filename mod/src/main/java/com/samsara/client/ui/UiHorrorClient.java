package com.samsara.client.ui;

import com.samsara.system.celestial.SolarFractureManager;
import com.samsara.system.ui.UiHorrorSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Kinh dị tầng giao diện - phía CLIENT:
 *  - Hiện toast thành tựu giả do server gửi (UiHorrorSystem.FAKE_ADVANCEMENT).
 *  - Menu tạm dừng ở Stage 4: phủ nhiễu băng VHS nhè nhẹ, và nút "Lưu & Thoát" thỉnh thoảng chớp thành
 *    "Tạo thế giới mới" (đúng câu kết của tài liệu). Bấm vẫn lưu & thoát như thường.
 * Dòng F3 lạ nằm ở DebugHudMixin.
 */
@Environment(EnvType.CLIENT)
public class UiHorrorClient {
    private static final Text NEW_WORLD = Text.translatable("selectWorld.create");
    private static final Random RANDOM = new Random();

    // ---------------- Nam_1998 trong bảng Tab ----------------
    private static net.minecraft.client.network.PlayerListEntry ghostEntry = null;

    /** Mục bảng Tab giả của Nam_1998 khi hắn đang "có mặt" (null nếu không). */
    public static net.minecraft.client.network.PlayerListEntry getGhostEntry() {
        return ghostEntry;
    }

    // ---------------- Menu chính sau đoạn kết ----------------
    private static final java.nio.file.Path STATE_FILE = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("samsara_client_state.txt");
    private static final Text EGG_READY = Text.translatable("samsara.ui_horror_client.qua_trung_tiep_theo");

    /** Gọi khi người chơi xem xong đoạn kết (NoSignalScreen). Lưu ra file để còn nhớ sau khi tắt game. */
    public static void markEndingSeen() {
        try {
            java.nio.file.Files.writeString(STATE_FILE, "ending_seen=true");
        } catch (java.io.IOException ignored) {
        }
    }

    public static boolean hasSeenEnding() {
        try {
            return java.nio.file.Files.exists(STATE_FILE) && java.nio.file.Files.readString(STATE_FILE).contains("ending_seen=true");
        } catch (java.io.IOException e) {
            return false;
        }
    }

    public static void clearEndingSeen() {
        try {
            java.nio.file.Files.deleteIfExists(STATE_FILE);
        } catch (java.io.IOException ignored) {
        }
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(UiHorrorSystem.GHOST_PRESENCE, (client, handler, buf, sender) -> {
            boolean present = buf.readBoolean();
            client.execute(() -> ghostEntry = present ? new net.minecraft.client.network.PlayerListEntry(
                    new com.mojang.authlib.GameProfile(com.samsara.system.cycle.NamGhostSystem.NAM_UUID, UiHorrorSystem.GHOST_NAME),
                    false) : null);
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ghostEntry = null);
        ClientPlayNetworking.registerGlobalReceiver(UiHorrorSystem.ENDING_MENU_FLAG, (client, handler, buf, sender) -> {
            boolean seen = buf.readBoolean();
            client.execute(() -> {
                if (seen) markEndingSeen();
                else clearEndingSeen();
            });
        });
        // Cột sóng chập chờn: lúc mạnh, lúc mất hẳn
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (ghostEntry != null && RANDOM.nextInt(15) == 0) {
                int[] pings = {40, 120, 260, 480, 900, -1};
                ((com.samsara.mixin.PlayerListEntryInvoker) ghostEntry).samsara$setLatency(pings[RANDOM.nextInt(pings.length)]);
            }
        });

        // Stage 1+: đang nằm ngủ, nút "Rời giường" thỉnh thoảng chớp thành "ĐỪNG NGỦ"
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof net.minecraft.client.gui.screen.SleepingChatScreen) || SolarFractureManager.clientStage < 1) return;
            ButtonWidget leave = findButton(screen, "multiplayer.stopSleeping");
            if (leave == null) return;
            Text original = leave.getMessage();
            int[] ticks = {0};
            int offset = RANDOM.nextInt(40);
            ScreenEvents.afterTick(screen).register(s -> {
                ticks[0]++;
                boolean flash = (ticks[0] + offset) % 50 >= 47;
                leave.setMessage(flash ? Text.translatable("samsara.ui_horror_client.dung_ngu") : original);
            });
        });

        // Sau đoạn kết: menu chính đổi câu chào, nút Chơi đơn thỉnh thoảng chớp thành "Tạo thế giới mới"
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof net.minecraft.client.gui.screen.TitleScreen) || !hasSeenEnding()) return;
            ((com.samsara.mixin.TitleScreenAccessor) screen).samsara$setSplashText(
                    new net.minecraft.client.gui.screen.SplashTextRenderer(EGG_READY.getString()));
            ButtonWidget single = findButton(screen, "menu.singleplayer");
            if (single == null) return;
            Text original = single.getMessage();
            int[] ticks = {0};
            ScreenEvents.afterTick(screen).register(s -> {
                ticks[0]++;
                single.setMessage(ticks[0] % 80 >= 76 ? NEW_WORLD : original);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(UiHorrorSystem.FAKE_ADVANCEMENT, (client, handler, buf, sender) -> {
            String title = buf.readString();
            Identifier itemId = buf.readIdentifier();
            client.execute(() -> showAdvancement(client, title, itemId));
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof GameMenuScreen) || SolarFractureManager.clientStage < 4) return;
            ButtonWidget quit = findQuitButton(screen);
            int[] ticks = {0};
            Text original = quit != null ? quit.getMessage() : null;

            ScreenEvents.afterTick(screen).register(s -> {
                ticks[0]++;
                if (quit == null) return;
                // Cứ khoảng 3 giây lại chớp thành "Tạo thế giới mới" trong vài khung
                boolean flash = ticks[0] % 60 >= 56;
                quit.setMessage(flash ? NEW_WORLD : original);
            });
            ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) ->
                    drawMenuNoise(ctx, s.width, s.height, ticks[0] + delta));
        });
    }

    private static void showAdvancement(MinecraftClient client, String title, Identifier itemId) {
        ItemStack icon = new ItemStack(Registries.ITEM.get(itemId));
        client.getToastManager().add(new FakeAdvancementToast(title, icon));
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_TOAST_IN, 0.55f, 1.0f));
    }

    private static ButtonWidget findButton(net.minecraft.client.gui.screen.Screen screen, String key) {
        for (ClickableWidget w : Screens.getButtons(screen)) {
            if (w instanceof ButtonWidget b && b.getMessage().getContent() instanceof TranslatableTextContent t
                    && t.getKey().equals(key)) {
                return b;
            }
        }
        return null;
    }

    private static ButtonWidget findQuitButton(net.minecraft.client.gui.screen.Screen screen) {
        for (ClickableWidget w : Screens.getButtons(screen)) {
            if (w instanceof ButtonWidget b && b.getMessage().getContent() instanceof TranslatableTextContent t
                    && (t.getKey().equals("menu.returnToMenu") || t.getKey().equals("menu.disconnect"))) {
                return b;
            }
        }
        return null;
    }

    /** Nhiễu nhẹ trên menu: dải tracking lăn chậm, vài mẩu nhiễu xám, thỉnh thoảng một vạch tối. */
    private static void drawMenuNoise(DrawContext ctx, int w, int h, float t) {
        RANDOM.setSeed((long) t * 31L);
        int bandH = 14;
        int bandY = (int) ((t * 1.2f) % (h + bandH * 2)) - bandH;
        ctx.fill(0, bandY, w, bandY + bandH, 0x22000000);
        for (int i = 0; i < 40; i++) {
            int y = bandY + RANDOM.nextInt(bandH);
            int x = RANDOM.nextInt(w);
            int g = 80 + RANDOM.nextInt(80);
            ctx.fill(x, y, x + 6 + RANDOM.nextInt(30), y + 1, (0x50 << 24) | (g << 16) | (g << 8) | g);
        }
        for (int i = 0; i < 90; i++) {
            int g = 90 + RANDOM.nextInt(100);
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            ctx.fill(x, y, x + 1, y + 1, (0x30 << 24) | (g << 16) | (g << 8) | g);
        }
        if (RANDOM.nextFloat() < 0.08f) {
            int y = RANDOM.nextInt(h);
            ctx.fill(0, y, w, y + 1, 0x60000000);
        }
    }
}
