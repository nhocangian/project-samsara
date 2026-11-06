package com.samsara.client;

import com.samsara.system.celestial.SolarFractureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.MathHelper;

/**
 * Nhận giai đoạn Mặt Trời Hố Đen từ server (SolarFractureManager.SUN_STAGE_PACKET)
 * để WorldRendererMixin vẽ đúng texture, kể cả khi chơi trên máy chủ riêng.
 */
@Environment(EnvType.CLIENT)
public class SolarStageClientSync {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SolarFractureManager.SUN_STAGE_PACKET, (client, handler, buf, responseSender) -> {
            int stage = MathHelper.clamp(buf.readVarInt(), SolarFractureManager.MIN_STAGE, SolarFractureManager.MAX_STAGE);
            client.execute(() -> {
                SolarFractureManager.clientStage = stage;
                client.updateWindowTitle(); // tiêu đề cửa sổ "SAMSARA-REL" từ Stage 1 (WindowTitleMixin)
            });
        });

        // Rời server -> về lại Mặt Trời mặc định cho tới khi server mới gửi giai đoạn của nó
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SolarFractureManager.clientStage = 0;
            client.execute(client::updateWindowTitle);
        });
    }
}
