package com.samsara.client.womb;

import com.samsara.client.WombRouteClient;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraParticles;
import com.samsara.system.womb.WombPacingSystem;
import com.samsara.system.womb.WombRoute;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Không khí chương III (Những Người Khổng Lồ): ký tự phù phép đỏ phát sáng (RuneParticle) bay lên từ mặt nước quanh
 * người chơi, dày hơn quanh ba khổng thần. Chỉ chạy phía client, chỉ trong đoạn tuyến của chương III.
 * Lệnh test: /samsara test womb_route tp 1 (tới Steve 2009).
 */
public final class RuneAmbienceClient {
    private RuneAmbienceClient() {}

    private static final Random RANDOM = Random.create();
    private static final int PER_TICK = 3;
    private static final double RADIUS = 30.0;
    private static final int TITAN_PER_TICK = 3;
    private static final double TITAN_NEAR = 60.0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(RuneAmbienceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null || client.isPaused()) return;
        if (!client.world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) return;
        BlockPos origin = WombRouteClient.getOrigin();
        if (origin == null) return;

        double px = client.player.getX();
        double pz = client.player.getZ();
        double progress = WombRoute.progress(origin, px, pz);
        if (progress < WombPacingSystem.TITANS_START - 4
                || progress > WombRoute.length(origin) - WombPacingSystem.SPIRE_MARGIN) return;

        // Mặc niệm: đi ngang khổng thần thì ký tự thưa lại còn một phần ba
        double silence = WombRoute.titanSilence(origin, progress);
        int perTick = silence < 0.5 ? 1 : PER_TICK;
        int titanPerTick = silence < 0.5 ? 1 : TITAN_PER_TICK;

        // Rải quanh người chơi, bốc lên từ mặt nước
        for (int i = 0; i < perTick; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2.0;
            double r = 3.0 + Math.sqrt(RANDOM.nextDouble()) * RADIUS;
            client.world.addParticle(SamsaraParticles.RUNE, px + Math.cos(a) * r,
                    WombRoute.SURFACE_Y - 0.2 + RANDOM.nextDouble() * 3.0, pz + Math.sin(a) * r, 0, 0, 0);
        }

        // Quanh thân khổng thần ở gần: bốc lên dọc thân tượng
        WombRoute.Stop[] titans = {WombRoute.Stop.TITAN_STEVE, WombRoute.Stop.TITAN_CREEPER, WombRoute.Stop.TITAN_ARSONIST};
        for (WombRoute.Stop stop : titans) {
            BlockPos l = stop.landmark(origin);
            double dx = l.getX() + 0.5 - px;
            double dz = l.getZ() + 0.5 - pz;
            if (dx * dx + dz * dz > TITAN_NEAR * TITAN_NEAR) continue;
            for (int i = 0; i < titanPerTick; i++) {
                double a = RANDOM.nextDouble() * Math.PI * 2.0;
                double r = Math.sqrt(RANDOM.nextDouble()) * 14.0;
                client.world.addParticle(SamsaraParticles.RUNE, l.getX() + 0.5 + Math.cos(a) * r,
                        WombRoute.SURFACE_Y + RANDOM.nextDouble() * 24.0, l.getZ() + 0.5 + Math.sin(a) * r, 0, 0, 0);
            }
        }
    }
}
