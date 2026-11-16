package com.samsara.system.overworld;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đuốc ở phía trước (hang động Overworld): thỉnh thoảng ở đoạn hang người chơi chưa tới, chỗ còn tối om, đã có sẵn một
 * ngọn đuốc cắm trên vách, như có ai vừa đi trước một bước. Đuốc được cắm lúc chỗ đó nằm ngoài tầm mắt (lệch 50-80° khỏi
 * hướng nhìn, cách 10-22 khối), nên người chơi chỉ thấy nó khi quay sang. Là đuốc thật, phá ra được, ở lại luôn.
 * Mỗi người chừng 6-10 phút dưới hang một lần; phím đạo diễn gọi ngay.
 */
public final class CaveTorchAhead {
    private CaveTorchAhead() {}

    private static final int FIRST_DELAY = 2400;
    private static final int MIN_GAP = 7200;
    private static final int EXTRA_GAP = 4800;
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CaveTorchAhead::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 40 != 0 || FilmingState.scripted(server)) return;
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!Caves.underground(player)) continue;
            long next = NEXT.computeIfAbsent(player.getUuid(), id -> now + FIRST_DELAY);
            if (now < next) continue;
            if (place(player, 10)) NEXT.put(player.getUuid(), now + MIN_GAP + player.getRandom().nextInt(EXTRA_GAP));
        }
    }

    /** Tìm một mặt vách tối ngoài tầm mắt rồi cắm đuốc. */
    private static boolean place(ServerPlayerEntity player, double minDist) {
        ServerWorld world = player.getServerWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        for (int i = 0; i < 24; i++) {
            double side = world.random.nextBoolean() ? 1 : -1;
            double a = Math.toRadians(50 + world.random.nextDouble() * 30) * side;
            Vec3d flat = look.multiply(1, 0, 1).normalize();
            Vec3d dir = new Vec3d(flat.x * Math.cos(a) - flat.z * Math.sin(a), (world.random.nextDouble() - 0.6) * 0.5,
                    flat.x * Math.sin(a) + flat.z * Math.cos(a)).normalize();
            BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(22)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            Direction face = hit.getSide();
            if (!face.getAxis().isHorizontal()) continue;
            BlockPos wall = hit.getBlockPos();
            BlockPos at = wall.offset(face);
            if (at.getSquaredDistance(eye) < minDist * minDist) continue;
            if (!world.isAir(at) || !world.getBlockState(wall).isOpaqueFullCube(world, wall)) continue;
            if (world.getLightLevel(LightType.BLOCK, at) > 0) continue;   // chỗ đã có ánh sáng: người chơi từng tới
            BlockState torch = Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, face);
            if (!torch.canPlaceAt(world, at)) continue;
            world.setBlockState(at, torch, 3);
            return true;
        }
        return false;
    }

    /** Phím đạo diễn: cắm ngay một ngọn đuốc như trên, gần hơn được (hang hẹp lúc quay); cần đang ở dưới hang. */
    public static boolean cue(ServerPlayerEntity player) {
        return Caves.underground(player) && place(player, 5);
    }
}
