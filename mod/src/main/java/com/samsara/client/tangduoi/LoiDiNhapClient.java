package com.samsara.client.tangduoi;

import com.samsara.client.CamcorderClientManager;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.tangduoi.LoiDiNhapBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.random.Random;

/** Đoạn 5 của tầng 2: ngắm máy quay chỉ thấy nhiễu trắng, phải hạ máy xuống và đi theo các tấm bảng. */
public final class LoiDiNhapClient {
    private LoiDiNhapClient() {}

    private static final Random RANDOM = Random.create();

    public static boolean inStaticZone(MinecraftClient client) {
        return client.player != null && client.world != null
                && client.world.getRegistryKey() == SamsaraDimensions.LOI_DI_NHAP_WORLD_KEY
                && LoiDiNhapBuilder.isStaticZone(client.player.getZ());
    }

    public static void register() {
        HudRenderCallback.EVENT.register(LoiDiNhapClient::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!CamcorderClientManager.isFullyAimed() || !inStaticZone(client)) return;
        float amount = LoiDiNhapBuilder.staticAmount(client.player.getZ());
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        // Vào sâu thì đặc dần; lúc đầu còn loé qua được hình phía sau
        int alpha = (int) (240 * amount * amount * (0.82f + 0.18f * RANDOM.nextFloat()));
        ctx.fill(0, 0, w, h, alpha << 24 | 0xC4C4C4);
        int lines = (int) (900 * amount);
        for (int i = 0; i < lines; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            int len = 2 + RANDOM.nextInt(26);
            int g = RANDOM.nextInt(256);
            ctx.fill(x, y, x + len, y + 1, 0xFF000000 | g << 16 | g << 8 | g);
        }
    }
}
