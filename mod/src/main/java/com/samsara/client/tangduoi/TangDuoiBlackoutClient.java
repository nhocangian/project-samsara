package com.samsara.client.tangduoi;

import com.samsara.system.tangduoi.OKhoaHam;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;

/** Màn đen khi cả căn hầm mất điện lúc mở khoá. Vẽ lớp phủ vì shader Tầng Dưới bỏ qua hiệu ứng mù của vanilla. */
@Environment(EnvType.CLIENT)
public final class TangDuoiBlackoutClient {
    private TangDuoiBlackoutClient() {}

    private static final int FADE_TICKS = 6;
    private static int remaining = 0;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OKhoaHam.BLACKOUT_PACKET, (client, handler, buf, sender) -> {
            int ticks = buf.readInt();
            client.execute(() -> remaining = ticks);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (remaining > 0) remaining--;
        });
        HudRenderCallback.EVENT.register(TangDuoiBlackoutClient::render);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        if (remaining <= 0) return;
        float a = Math.min(1.0f, remaining / (float) FADE_TICKS);
        ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), ((int) (a * 255.0f)) << 24);
    }
}
