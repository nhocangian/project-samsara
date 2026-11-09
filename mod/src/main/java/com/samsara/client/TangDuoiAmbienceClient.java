package com.samsara.client;

import com.samsara.registry.SamsaraDimensions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Âm nền Tầng Dưới: tiếng ù đèn huỳnh quang và tiếng nước nhỏ giọt ở xa. Không có nhạc.
 * Toàn bộ dùng âm vanilla đổi cao độ.
 */
@Environment(EnvType.CLIENT)
public final class TangDuoiAmbienceClient {
    private TangDuoiAmbienceClient() {}

    private static final Random RANDOM = Random.create();
    private static int humTicks = 0;
    private static int dripTicks = 60;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TangDuoiAmbienceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null || client.isPaused()) return;
        if (client.world.getRegistryKey() != SamsaraDimensions.TANG_DUOI_WORLD_KEY) return;

        client.getMusicTracker().stop();
        // Phòng ngủ 2018 im phăng phắc
        if (com.samsara.system.tangduoi.TangDuoiRoom.isInRoom(client.player.getX(), client.player.getZ())) return;
        // Lúc mất điện sau khi mở khoá: đèn gần lối vào đang tắt thì không có tiếng ù
        if (!client.world.getBlockState(com.samsara.system.tangduoi.DuongOngBuilder.entryLamp(0)).isOf(com.samsara.registry.SamsaraBlocks.DEN_HUYNH_QUANG)) {
            humTicks = 0;
            return;
        }
        if (--humTicks <= 0) {
            humTicks = 72;
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_BEACON_AMBIENT, 1.9f, 0.16f));
        }
        if (--dripTicks <= 0) {
            dripTicks = 70 + RANDOM.nextInt(190);
            client.getSoundManager().play(PositionedSoundInstance.master(
                    SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER, 0.6f + RANDOM.nextFloat() * 0.5f, 0.1f));
        }
    }
}
