package com.samsara.system.overworld;

import com.samsara.block.MemorySunflowerBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.system.cycle.EggCycleState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hoa quay theo người (Overworld): hướng dương ngoài tự nhiên luôn quay về hướng đông. Thỉnh thoảng một bông nằm
 * ngoài tầm nhìn lặng lẽ quay mặt về phía người chơi; lúc người chơi liếc tới thì thấy nó đang nhìn mình. Người chơi
 * quay đi một lúc thì nó trở lại hướng đông như chưa có gì. Hiếm ở Stage 0, nhiều bông hơn từ Stage 2.
 *
 * <p>Quay bằng cách đổi bông vanilla thành {@code memory_sunflower} (cùng model, xoay được) rồi đổi lại; hoa xoay để
 * quên lại (ví dụ khi tắt game) mà quay hướng đông thì trông y như hoa vanilla, phá ra vẫn rơi hoa hướng dương.
 */
public final class OverworldSunflowerGaze {
    private OverworldSunflowerGaze() {}

    private static final int RADIUS = 16;
    /** Nằm trong nón này quanh hướng nhìn thì coi là người chơi đang thấy. */
    private static final double VIEW_COS = Math.cos(Math.toRadians(55));
    /** Đã được nhìn thấy rồi, ra khỏi tầm nhìn chừng này tick thì quay về hướng đông. */
    private static final int RETURN_AFTER = 40;
    /** Chưa ai nhìn thấy thì cũng tự quay về sau chừng này tick. */
    private static final int GIVE_UP = 1200;

    private static final class Turned {
        final ServerPlayerEntity player;
        final long since;
        boolean seen;
        long lastSeen;

        Turned(ServerPlayerEntity player, long since) {
            this.player = player;
            this.since = since;
        }
    }

    private static final Map<BlockPos, Turned> TURNED = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(OverworldSunflowerGaze::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ServerWorld world = server.getOverworld();
            for (BlockPos p : new ArrayList<>(TURNED.keySet())) restore(world, p);
            TURNED.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;
        long now = world.getTime();

        // Bông đang quay: ghi lúc được nhìn thấy, quay về khi người chơi đã thấy rồi quay đi
        if (now % 5 == 0) {
            for (Iterator<Map.Entry<BlockPos, Turned>> it = TURNED.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<BlockPos, Turned> e = it.next();
                Turned t = e.getValue();
                boolean gone = t.player.isRemoved() || t.player.getWorld() != world
                        || t.player.squaredDistanceTo(Vec3d.ofCenter(e.getKey())) > 40 * 40;
                boolean visible = !gone && inView(t.player, e.getKey());
                if (visible) {
                    t.seen = true;
                    t.lastSeen = now;
                }
                boolean back = gone || (t.seen && !visible && now - t.lastSeen >= RETURN_AFTER)
                        || (!t.seen && now - t.since >= GIVE_UP);
                if (back) {
                    restore(world, e.getKey());
                    it.remove();
                }
            }
        }

        if (now % 20 != 0 || FilmingState.scripted(server)) return;
        int stage = EggCycleState.get(server).getStage();
        int maxTurned = stage >= 2 ? 3 : stage >= 1 ? 2 : 1;
        float chance = stage >= 2 ? 0.05f : 0.015f;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            int mine = 0;
            for (Turned t : TURNED.values()) if (t.player == player) mine++;
            if (mine >= maxTurned) continue;
            for (BlockPos p : findSunflowers(world, player)) {
                if (mine >= maxTurned) break;
                if (inView(player, p) || world.random.nextFloat() >= chance) continue;
                if (turn(world, p, player, now)) mine++;
            }
        }
    }

    /** Gốc (nửa dưới) các cây hướng dương vanilla quanh người chơi, cách 4..16 khối. */
    private static List<BlockPos> findSunflowers(ServerWorld world, ServerPlayerEntity player) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int d2 = x * x + z * z;
                if (d2 < 16 || d2 > RADIUS * RADIUS) continue;
                for (int y = -6; y <= 6; y++) {
                    m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    BlockState s = world.getBlockState(m);
                    if (s.isOf(Blocks.SUNFLOWER) && s.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER) out.add(m.toImmutable());
                }
            }
        }
        return out;
    }

    private static boolean inView(ServerPlayerEntity player, BlockPos flower) {
        Vec3d to = new Vec3d(flower.getX() + 0.5, flower.getY() + 1.5, flower.getZ() + 0.5).subtract(player.getEyePos());
        if (to.lengthSquared() < 1.0e-4) return true;
        return to.normalize().dotProduct(player.getRotationVec(1.0f)) > VIEW_COS;
    }

    /** Đổi bông vanilla tại {@code lower} thành bông xoay được, quay về phía người chơi. */
    private static boolean turn(ServerWorld world, BlockPos lower, ServerPlayerEntity player, long now) {
        BlockPos upper = lower.up();
        if (!world.getBlockState(upper).isOf(Blocks.SUNFLOWER) || TURNED.containsKey(lower)) return false;
        double dx = player.getX() - (lower.getX() + 0.5), dz = player.getZ() - (lower.getZ() + 0.5);
        Direction face = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        if (face == Direction.EAST) return false;   // vốn đã nhìn hướng đó, quay cũng không ai thấy khác
        BlockState s = SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState().with(MemorySunflowerBlock.FACING, face);
        // 2|16: báo client, không cập nhật hàng xóm (để hai nửa cây cao không tự vỡ khi đổi lần lượt)
        world.setBlockState(lower, s.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        world.setBlockState(upper, s.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
        TURNED.put(lower, new Turned(player, now));
        return true;
    }

    private static void restore(ServerWorld world, BlockPos lower) {
        if (!world.isChunkLoaded(lower.getX() >> 4, lower.getZ() >> 4)) return;
        if (!world.getBlockState(lower).isOf(SamsaraBlocks.MEMORY_SUNFLOWER)) return;
        BlockState s = Blocks.SUNFLOWER.getDefaultState();
        world.setBlockState(lower, s.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), 2 | 16);
        if (world.getBlockState(lower.up()).isOf(SamsaraBlocks.MEMORY_SUNFLOWER)) {
            world.setBlockState(lower.up(), s.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), 2 | 16);
        }
    }

    /** Phím đạo diễn: mọi bông hướng dương quanh người chơi đang nằm ngoài tầm mắt quay về phía người chơi. */
    public static int cue(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return 0;
        int n = 0;
        for (BlockPos p : findSunflowers(world, player)) {
            if (!inView(player, p) && turn(world, p, player, world.getTime())) n++;
        }
        return n;
    }

    /** Lệnh test: mọi bông hướng dương vanilla quanh người chơi quay về phía người chơi ngay (kể cả bông đang nhìn). */
    public static int testNow(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return 0;
        int n = 0;
        for (BlockPos p : findSunflowers(world, player)) if (turn(world, p, player, world.getTime())) n++;
        return n;
    }
}
