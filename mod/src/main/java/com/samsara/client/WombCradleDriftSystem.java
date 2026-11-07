package com.samsara.client;

import com.samsara.registry.SamsaraDimensions;
import com.samsara.registry.SamsaraParticles;
import com.samsara.system.womb.WombPacingSystem;
import com.samsara.system.womb.WombRoute;
import com.samsara.system.womb.chapters.SaveDoorLayout;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Hạt môi trường của The Womb, theo chương của tuyến hành hương (không còn theo biome, không còn end rod / hạt cổng):
 *  - Trong căn phòng cửa (chương I-II): không có gì. Chưa thấy hố đen thì chưa có gì bị hút.
 *  - Ngoài phòng (từ cánh cửa trắng tới trước chương IV): bụi vàng kem (MoteParticle) bốc lên từ mặt nước và bị hút
 *    về Gargantua, tăng tốc và kéo thành vệt. Chương III có thêm ký tự đỏ (client/womb/RuneAmbienceClient).
 *  - Chương IV (Tháp): bụi dày hơn và xuất phát nhanh hơn: lực hút mạnh dần khi tới gần Quả Trứng.
 */
public class WombCradleDriftSystem {
    private static final Random RAND = new Random();

    private static final double RADIUS = 30.0;
    private static final double BASE_SPEED = 0.03;
    private static final double SPIRE_SPEED = 0.055;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        PlayerEntity player = client.player;
        if (world == null || player == null || client.isPaused()) return;
        if (!world.getRegistryKey().equals(SamsaraDimensions.THE_WOMB_WORLD_KEY)) return;
        BlockPos origin = WombRouteClient.getOrigin();
        if (origin == null) return;
        if (SaveDoorLayout.insideRoom(origin, player.getBlockX(), player.getBlockZ(), 0)) return;

        double progress = WombRoute.progress(origin, player.getX(), player.getZ());
        boolean spire = progress > WombRoute.length(origin) - WombPacingSystem.SPIRE_MARGIN;
        int count = spire ? 3 + RAND.nextInt(2) : 1 + RAND.nextInt(2);
        double speed = spire ? SPIRE_SPEED : BASE_SPEED;

        for (int i = 0; i < count; i++) {
            double a = RAND.nextDouble() * Math.PI * 2.0;
            double r = 2.0 + Math.sqrt(RAND.nextDouble()) * RADIUS;
            double px = player.getX() + Math.cos(a) * r;
            double pz = player.getZ() + Math.sin(a) * r;
            // Phần lớn bốc từ mặt nước, một ít đang lơ lửng giữa chừng
            double py = RAND.nextFloat() < 0.7f ? WombRoute.SURFACE_Y - 0.1 + RAND.nextDouble() * 1.5
                    : WombRoute.SURFACE_Y + 2.0 + RAND.nextDouble() * 14.0;
            world.addParticle(SamsaraParticles.MOTE, px, py, pz, 0.0, speed, 0.0);
        }
    }
}
