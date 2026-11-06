package com.samsara.client.ending;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Màn cuối của đoạn kết: tín hiệu OBS tắt phụt (Video 9) rồi câu kết của Channy (mục V).
 *
 *   0 - 110 tick   NO SIGNAL: nhiễu tuyết VHS, chữ "NO SIGNAL", timecode đứng yên, "OBS - REC STOPPED".
 *   110 - 130      đen tuyền.
 *   130 - 330      câu kết màu vàng hiện dần, giữ, rồi tắt dần.
 *   350            lưu & thoát về màn hình chính.
 * Không đóng được bằng Esc.
 */
@Environment(EnvType.CLIENT)
public class NoSignalScreen extends Screen {
    private static final int NOISE_END = 110;
    private static final int QUOTE_START = 130;
    private static final int QUOTE_END = 330;
    private static final int EXIT_TICK = 350;
    private static final String QUOTE_1 = "samsara.no_signal_screen.dang_sau_nut_luu";
    private static final String QUOTE_2 = "samsara.no_signal_screen.luon_luon_la_nut";

    private final Random random = Random.create();
    private int ticks = 0;
    private boolean exiting = false;

    public NoSignalScreen() {
        super(Text.literal("NO SIGNAL"));
    }

    @Override
    protected void init() {
        if (ticks == 0 && client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f));
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (client == null) return;
        if (ticks < NOISE_END && ticks % 4 == 0) {
            // Rè tĩnh điện của băng hết tín hiệu
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_SAND_BREAK, 0.4f + random.nextFloat() * 0.3f, 0.12f));
        }
        if (ticks >= EXIT_TICK && !exiting) {
            exiting = true;
            exitToTitle(client);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xFF000000);
        float t = ticks + delta;

        if (t < NOISE_END) {
            // Nhiễu tuyết thô theo ô 4 px
            int cell = 4;
            for (int y = 0; y < height; y += cell) {
                for (int x = 0; x < width; x += cell) {
                    int g = random.nextInt(170);
                    ctx.fill(x, y, x + cell, y + cell, 0xFF000000 | (g << 16) | (g << 8) | g);
                }
            }
            // Vạch nhiễu ngang lăn
            int band = (int) ((t * 6) % (height + 40)) - 20;
            ctx.fill(0, band, width, band + 10, 0x55FFFFFF);

            // Khung chữ NO SIGNAL (xanh lam kiểu đầu VHS)
            int bw = 150;
            int bh = 34;
            int bx = width / 2 - bw / 2;
            int by = height / 2 - bh / 2;
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF0B1FA8);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NO SIGNAL"), width / 2, by + 13, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal("OBS ■ REC STOPPED"), 12, 12, 0xFFFF3B3B);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Nam_1998_LOG_09.mp4   00:47:13:09"), 12, height - 20, 0xFFE0E0E0);
            return;
        }

        if (t >= QUOTE_START && t <= QUOTE_END) {
            float local = t - QUOTE_START;
            float total = QUOTE_END - QUOTE_START;
            float alpha = MathHelper.clamp(Math.min(local / 40.0f, (total - local) / 60.0f), 0.0f, 1.0f);
            int a = Math.max(4, (int) (alpha * 255));
            int color = (a << 24) | 0xF2D27A;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable(QUOTE_1), width / 2, height / 2 - 8, color);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable(QUOTE_2), width / 2, height / 2 + 6, color);
        }
    }

    /** Giống nút "Lưu & Thoát" của menu tạm dừng, rồi tạo luôn world chu kỳ mới (chơi đơn). */
    private static void exitToTitle(MinecraftClient client) {
        com.samsara.client.ui.UiHorrorClient.markEndingSeen();
        boolean singleplayer = client.isInSingleplayer();
        // Ghi lại thiết lập world trước khi thoát, để tạo world "Chu Kỳ #N+1" ngay sau đó
        NewCycleClient.Plan nextCycle = singleplayer ? NewCycleClient.capture(client) : null;
        if (client.world != null) {
            client.world.disconnect();
        }
        if (singleplayer) {
            client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        } else {
            client.disconnect();
        }
        if (nextCycle != null && NewCycleClient.start(client, nextCycle)) return;
        client.setScreen(new TitleScreen());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
