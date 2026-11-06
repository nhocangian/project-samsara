package com.samsara.system.memory;

import com.samsara.block.MemorySunflowerBlock;
import com.samsara.registry.SamsaraBlocks;
import com.samsara.registry.SamsaraDimensions;
import com.samsara.worldgen.memory.NamMemoryLayout;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Ký Ức Của Nam: mọi bông hoa hướng dương quanh người chơi quay mặt về phía người chơi,
 * và quay theo khi người chơi di chuyển (kịch bản Video 7, cảnh 03).
 *
 * Mỗi {@link #INTERVAL} tick quét các cột hoa trong bán kính {@link #RADIUS}. Bông nào đang nhìn sai hướng
 * chỉ có {@link #TURN_CHANCE} cơ hội quay mỗi lượt, nên cả cánh đồng quay theo kiểu gợn sóng lệch nhịp
 * trong khoảng một hai giây, không đồng loạt như máy.
 */
public final class SunflowerGazeSystem {
    private static final int RADIUS = 48;
    private static final int INTERVAL = 10;
    private static final float TURN_CHANCE = 0.4f;
    /** Không quay hoa ngay dưới chân (tránh hoa giật liên tục khi người chơi đứng giữa các bông). */
    private static final double MIN_DIST_SQ = 1.5 * 1.5;

    private SunflowerGazeSystem() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != SamsaraDimensions.NAM_MEMORY_WORLD_KEY) return;
            if (world.getTime() % INTERVAL != 0) return;
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isSpectator()) continue;
                turnToward(world, player.getX(), player.getZ(), RADIUS, TURN_CHANCE);
            }
        });
    }

    /**
     * Quay các bông hoa trong bán kính về phía điểm (px, pz).
     * @param chance xác suất mỗi bông sai hướng được quay trong lượt này (1 = quay hết ngay)
     * @return số bông đã quay
     */
    public static int turnToward(ServerWorld world, double px, double pz, int radius, float chance) {
        int lower = NamMemoryLayout.GROUND_Y + 1;
        int cx = (int) Math.floor(px);
        int cz = (int) Math.floor(pz);
        Random random = world.getRandom();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int turned = 0;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                pos.set(x, lower, z);
                BlockState state = world.getBlockState(pos);
                if (!state.isOf(SamsaraBlocks.MEMORY_SUNFLOWER)) continue;

                double dx = px - (x + 0.5);
                double dz = pz - (z + 0.5);
                double distSq = dx * dx + dz * dz;
                if (distSq < MIN_DIST_SQ || distSq > (double) radius * radius) continue;

                Direction want = Math.abs(dx) > Math.abs(dz)
                        ? (dx > 0 ? Direction.EAST : Direction.WEST)
                        : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                if (state.get(MemorySunflowerBlock.FACING) == want) continue;
                if (chance < 1.0f && random.nextFloat() >= chance) continue;

                // Cả hai nửa của cây hoa cao đều mang FACING; cờ 2|16: báo client, không cập nhật hàng xóm
                world.setBlockState(pos, state.with(MemorySunflowerBlock.FACING, want), 2 | 16);
                BlockPos up = pos.up();
                BlockState upper = world.getBlockState(up);
                if (upper.isOf(SamsaraBlocks.MEMORY_SUNFLOWER)) {
                    world.setBlockState(up, upper.with(MemorySunflowerBlock.FACING, want), 2 | 16);
                }
                turned++;
            }
        }
        return turned;
    }
}
