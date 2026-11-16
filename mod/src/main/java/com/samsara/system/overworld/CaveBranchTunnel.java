package com.samsara.system.overworld;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Đường hầm nhánh không phải của mình (hang động Overworld): hiếm khi, trong vách hang ngoài tầm mắt người chơi mở ra
 * một đường hầm 1x2 thẳng tắp đào sâu vào đá kiểu đào nhánh tìm kim cương, cứ 8 khối một ngọn đuốc, cuối hầm là ngõ
 * cụt với một cái cuốc đá gần gãy bỏ lại (không biến mất). Đúng thói quen của một người chơi thật, mà người chơi biết
 * mình chưa từng đào chỗ này. Mỗi người chừng 20-30 phút dưới hang một lần; phím đạo diễn gọi ngay.
 */
public final class CaveBranchTunnel {
    private CaveBranchTunnel() {}

    private static final int FIRST_DELAY = 9600;
    private static final int MIN_GAP = 24000;
    private static final int EXTRA_GAP = 12000;
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CaveBranchTunnel::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 100 != 0 || FilmingState.scripted(server)) return;
        long now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!Caves.underground(player)) continue;
            long next = NEXT.computeIfAbsent(player.getUuid(), id -> now + FIRST_DELAY);
            if (now < next) continue;
            if (carve(player, 6)) NEXT.put(player.getUuid(), now + MIN_GAP + player.getRandom().nextInt(EXTRA_GAP));
        }
    }

    /** Tìm một mặt vách ngoài tầm mắt (cách 6-16 khối) rồi đào hầm vào trong đá. */
    private static boolean carve(ServerPlayerEntity player, double minDist) {
        ServerWorld world = player.getServerWorld();
        Vec3d eye = player.getEyePos();
        Vec3d flat = player.getRotationVec(1.0f).multiply(1, 0, 1).normalize();
        for (int i = 0; i < 24; i++) {
            double side = world.random.nextBoolean() ? 1 : -1;
            double a = Math.toRadians(55 + world.random.nextDouble() * 70) * side;
            Vec3d dir = new Vec3d(flat.x * Math.cos(a) - flat.z * Math.sin(a), -0.15,
                    flat.x * Math.sin(a) + flat.z * Math.cos(a)).normalize();
            BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(16)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
            if (hit.getType() != HitResult.Type.BLOCK || !hit.getSide().getAxis().isHorizontal()) continue;
            BlockPos mouthAir = hit.getBlockPos().offset(hit.getSide());
            if (mouthAir.getSquaredDistance(eye) < minDist * minDist) continue;
            // Hạ xuống mặt sàn hang ngay trước vách
            BlockPos floor = mouthAir;
            for (int d = 0; d < 4 && world.isAir(floor.down()); d++) floor = floor.down();
            if (world.isAir(floor.down())) continue;
            Direction into = hit.getSide().getOpposite();
            if (build(world, floor.offset(into), into)) return true;
        }
        return false;
    }

    /** Đào hầm 1x2 từ {@code start} theo {@code dir}; dừng nếu gặp hang khác, nước, dung nham hay chỗ hổng dưới sàn. */
    private static boolean build(ServerWorld world, BlockPos start, Direction dir) {
        int want = 14 + world.random.nextInt(9);
        // Đi dọc hướng đào: khối nào đặc thì đào được; gặp chỗ hổng, chất lỏng, khối có dữ liệu (rương...) hay sàn
        // hổng thì dừng ở trước đó, để hầm luôn là ngõ cụt và không đào xuyên vào hang khác
        int length = 0;
        while (length < want) {
            BlockPos p = start.offset(dir, length);
            boolean ok = world.getBlockState(p.down()).isOpaqueFullCube(world, p.down());
            for (int y = 0; y < 3 && ok; y++) {
                BlockPos q = p.up(y);
                BlockState s = world.getBlockState(q);
                ok = s.isOpaqueFullCube(world, q) && s.getFluidState().isEmpty() && world.getBlockEntity(q) == null;
            }
            // Trần (y + 2) và hai bên cũng phải đặc, kẻo hầm mở ra hang bên cạnh
            ok = ok && world.getBlockState(p.offset(dir.rotateYClockwise())).isOpaqueFullCube(world, p)
                    && world.getBlockState(p.offset(dir.rotateYCounterclockwise())).isOpaqueFullCube(world, p);
            if (!ok) break;
            length++;
        }
        if (length < 10) return false;
        // Chừa 1 khối đá cuối cùng làm ngõ cụt
        length--;
        Direction side = dir.rotateYClockwise();
        for (int i = 0; i < length; i++) {
            BlockPos p = start.offset(dir, i);
            world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            world.setBlockState(p.up(), Blocks.AIR.getDefaultState(), 3);
            if (i % 8 == 4) {
                BlockState torch = Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, side.getOpposite());
                if (torch.canPlaceAt(world, p.up())) world.setBlockState(p.up(), torch, 3);
            }
        }
        // Cuốc đá gần gãy bỏ lại ở cuối hầm
        BlockPos end = start.offset(dir, length - 1);
        ItemStack pick = new ItemStack(Items.STONE_PICKAXE);
        pick.setDamage(pick.getMaxDamage() - 3);
        ItemEntity item = new ItemEntity(world, end.getX() + 0.5, end.getY() + 0.1, end.getZ() + 0.5, pick, 0, 0, 0);
        item.setNeverDespawn();
        world.spawnEntity(item);
        return true;
    }

    /** Phím đạo diễn: đào ngay một đường hầm như trên (cần đang ở dưới hang). */
    public static boolean cue(ServerPlayerEntity player) {
        return Caves.underground(player) && carve(player, 3);
    }
}
