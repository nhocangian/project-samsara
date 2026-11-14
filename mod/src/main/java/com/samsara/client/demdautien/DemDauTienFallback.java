package com.samsara.client.demdautien;

import com.samsara.client.trontim.TronTimFallback;
import com.samsara.system.demdautien.DemDauTienBuilder;
import com.samsara.system.demdautien.DemDauTienClock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;

/**
 * Tầng 4 "Đêm đầu tiên" khi không có shaderpack Samsara (tắt Iris hoặc dùng pack khác): shader world6 không chạy nên
 * không thấy vết rách, đảo hiện toàn bản mới. Để vẫn chơi được, mod vẽ viền vết rách bằng hạt bụi màu glitch (hồng tím /
 * xanh lơ / trắng) trên mặt đất, theo đúng mép mà server dùng ({@link DemDauTienClock#margin}), kể cả vết rách nhỏ ở cừu.
 * Cảnh cuối: hạt phủ khắp đảo theo vết rách nở ra, rồi màn hình trắng xoá (shader vốn làm việc này; lớp trắng vẽ
 * trên HUD nên ẩn HUD bằng F1 thì không thấy).
 * Khi pack Samsara đang chạy thì không làm gì.
 */
public final class DemDauTienFallback {
    private DemDauTienFallback() {}

    private static final DustParticleEffect[] COLORS = {
            new DustParticleEffect(new Vector3f(1.0f, 0.25f, 0.9f), 1.3f),
            new DustParticleEffect(new Vector3f(0.25f, 0.95f, 1.0f), 1.3f),
            new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.0f),
    };
    private static final Random RANDOM = Random.create();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DemDauTienFallback::tick);
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!active(mc)) return;
            long hour = DemDauTienClock.hour(DemDauTienClock.clientTimeOfDay(mc.world));
            float w = MathHelper.clamp((hour - DemDauTienClock.FINALE - DemDauTienClock.WHITE_AFTER) / 40.0f, 0.0f, 1.0f);
            if (w <= 0) return;
            int a = (int) (w * 255) << 24;
            ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), a | 0xFFFFFF);
        });
    }

    private static boolean active(MinecraftClient mc) {
        return mc.world != null && mc.player != null && DemDauTienClock.isFloor(mc.world) && !TronTimFallback.samsaraShadersOn();
    }

    private static void tick(MinecraftClient mc) {
        if (!active(mc) || mc.isPaused()) return;
        ClientWorld world = mc.world;
        long tod = DemDauTienClock.clientTimeOfDay(world);
        long hour = DemDauTienClock.hour(tod);
        if (hour >= DemDauTienClock.FINALE) {
            // Vết rách nở ra nuốt đảo: rải hạt theo mép đang lan khắp đảo
            for (int n = 0; n < 160; n++) {
                double a = RANDOM.nextDouble() * Math.PI * 2, r = Math.sqrt(RANDOM.nextDouble()) * 44;
                sample(world, Math.cos(a) * r, Math.sin(a) * r, tod, 1.5);
            }
            return;
        }
        for (double[] t : DemDauTienBuilder.TEARS) {
            for (int n = 0; n < 110; n++) {
                double a = RANDOM.nextDouble() * Math.PI * 2, r = t[3] * (0.2 + RANDOM.nextDouble() * 1.4);
                sample(world, t[0] + Math.cos(a) * r, t[2] + Math.sin(a) * r, tod, 0.35);
            }
        }
        int bits = DemDauTienClock.sheepBits(tod);
        for (int k = 0; k < DemDauTienBuilder.SHEEP.length; k++) {
            if ((bits & (1 << k)) == 0) continue;
            int[] s = DemDauTienBuilder.SHEEP[k];
            for (int n = 0; n < 12; n++) {
                double a = RANDOM.nextDouble() * Math.PI * 2, r = RANDOM.nextDouble() * 2.6;
                sample(world, s[0] + 0.5 + Math.cos(a) * r, s[1] + 0.5 + Math.sin(a) * r, tod, 0.3);
            }
        }
    }

    /** Điểm (x, z) trên mặt đất: nằm sát mép vết rách thì nhả một hạt. */
    private static void sample(ClientWorld world, double x, double z, long tod, double band) {
        int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, bx, bz);
        if (top <= world.getBottomY() + 1) return;
        // Dò xuống dưới tán lá (bản đồ độ cao gửi xuống client có tính cả lá) tới mặt đất thật
        net.minecraft.util.math.BlockPos.Mutable p = new net.minecraft.util.math.BlockPos.Mutable(bx, top - 1, bz);
        for (int i = 0; i < 12; i++) {
            net.minecraft.block.BlockState st = world.getBlockState(p);
            if (!st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) && !st.isIn(net.minecraft.registry.tag.BlockTags.LOGS)
                    && !st.isAir() && st.isOpaqueFullCube(world, p)) break;
            p.move(0, -1, 0);
        }
        top = p.getY() + 1;
        double m = DemDauTienClock.margin(x, top + 0.5, z, tod);
        if (Math.abs(m) > band) return;
        double y = top + 0.05 + RANDOM.nextDouble() * RANDOM.nextDouble() * 2.2;
        world.addParticle(COLORS[RANDOM.nextInt(COLORS.length)], x, y, z, 0.0, 0.004, 0.0);
    }
}
