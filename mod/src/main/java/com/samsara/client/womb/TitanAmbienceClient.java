package com.samsara.client.womb;

import com.samsara.client.WombRouteClient;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.system.womb.WombRoute;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Âm thanh thánh đường của chương III (Những Người Khổng Lồ), phía client:
 *  - Tiếng chuông trầm từ khổng thần gần nhất (trong {@link #BELL_RANGE} block), 20-30 giây một lần, kèm tiếng ngân
 *    dư âm: như chuông nhà thờ cho người đã khuất.
 *  - Tiếng ngân rất trầm liên tục khi đứng gần khổng thần (trong {@link #HUM_RANGE} block), như hợp xướng ở xa.
 * Âm vanilla hạ giọng; phát tại vị trí khổng thần nên nghe được hướng.
 */
public final class TitanAmbienceClient {
    private TitanAmbienceClient() {}

    private static final Random RANDOM = Random.create();
    private static final double BELL_RANGE = 110.0;
    private static final double HUM_RANGE = 50.0;

    private static int nextBell = 60;
    private static int nextHum = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TitanAmbienceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null || client.isPaused()) return;
        if (!client.world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) return;
        BlockPos origin = WombRouteClient.getOrigin();
        if (origin == null) return;

        // Khổng thần gần nhất
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (WombRoute.Stop stop : new WombRoute.Stop[]{WombRoute.Stop.TITAN_STEVE, WombRoute.Stop.TITAN_CREEPER, WombRoute.Stop.TITAN_ARSONIST}) {
            BlockPos l = stop.landmark(origin);
            double d = client.player.squaredDistanceTo(l.getX() + 0.5, client.player.getY(), l.getZ() + 0.5);
            if (d < best) {
                best = d;
                nearest = l;
            }
        }
        if (nearest == null) return;
        double dist = Math.sqrt(best);
        double x = nearest.getX() + 0.5;
        double y = WombRoute.SURFACE_Y + 20.0;
        double z = nearest.getZ() + 0.5;

        if (dist < BELL_RANGE && --nextBell <= 0) {
            nextBell = 400 + RANDOM.nextInt(200);
            play(client, SoundEvents.BLOCK_BELL_USE, x, y, z, 6.0f, 0.5f);
            play(client, SoundEvents.BLOCK_BELL_RESONATE, x, y, z, 4.0f, 0.5f);
        }
        if (dist < HUM_RANGE && --nextHum <= 0) {
            nextHum = 70 + RANDOM.nextInt(30);
            float volume = (float) (1.6 * (1.0 - dist / HUM_RANGE)) + 0.3f;
            play(client, SoundEvents.BLOCK_CONDUIT_AMBIENT, x, WombRoute.SURFACE_Y + 8.0, z, volume, 0.5f);
        }
    }

    private static void play(MinecraftClient client, SoundEvent sound, double x, double y, double z, float volume, float pitch) {
        client.getSoundManager().play(new PositionedSoundInstance(sound, SoundCategory.AMBIENT, volume, pitch,
                RANDOM, x, y, z));
    }
}
