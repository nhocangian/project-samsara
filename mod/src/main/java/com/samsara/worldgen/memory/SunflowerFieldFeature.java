package com.samsara.worldgen.memory;

import com.mojang.serialization.Codec;
import com.samsara.block.MemorySunflowerBlock;
import com.samsara.registry.SamsaraBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Phủ kín một chunk của chiều "Ký Ức Của Nam" bằng hoa hướng dương,
 * trừ con đường mòn (dirt path) và khoảng sân quanh nhà.
 * Placed feature không có placement modifier -> chạy đúng 1 lần mỗi chunk, origin = góc chunk.
 *
 * Hoa dùng MemorySunflowerBlock và luôn QUAY MẶT VỀ PHÍA NƠI NGƯỜI CHƠI TỈNH DẬY:
 * phía trước điểm xuất hiện (về phía nhà) hoa nhìn về nam, phía sau nhìn về bắc.
 * Đứng trên con đường mòn nhìn về nhà, cả màn hình là cánh hoa vàng đang nhìn bạn.
 */
public class SunflowerFieldFeature extends Feature<DefaultFeatureConfig> {
    public SunflowerFieldFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        int startX = context.getOrigin().getX() & ~15;
        int startZ = context.getOrigin().getZ() & ~15;
        plantChunk(context.getWorld(), startX, startZ, context.getRandom());
        return true;
    }

    /** Hướng mặt hoa tại cột (x, z): nhìn về điểm xuất hiện trên con đường mòn. */
    public static Direction facingAt(int x, int z) {
        return z <= NamMemoryLayout.ARRIVAL_Z ? Direction.SOUTH : Direction.NORTH;
    }

    public static void plantChunk(WorldAccess world, int startX, int startZ, Random random) {
        int ground = NamMemoryLayout.GROUND_Y;
        BlockState sunflower = SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                if (NamMemoryLayout.isPath(x, z)) {
                    world.setBlockState(pos.set(x, ground, z), Blocks.DIRT_PATH.getDefaultState(), 2);
                    continue;
                }
                pos.set(x, ground + 1, z);
                if (NamMemoryLayout.isClearing(x, z)) {
                    // Sân nhà: thỉnh thoảng có ngọn cỏ hoặc bông hoa nhỏ
                    float r = random.nextFloat();
                    if (r < 0.12f) world.setBlockState(pos, Blocks.GRASS.getDefaultState(), 2);
                    else if (r < 0.15f) world.setBlockState(pos, Blocks.DANDELION.getDefaultState(), 2);
                    continue;
                }
                TallPlantBlock.placeAt(world, sunflower.with(MemorySunflowerBlock.FACING, facingAt(x, z)), pos, 2);
            }
        }
    }

    /**
     * Trồng lại hoa cho các chunk đã sinh từ trước (lệnh test nam_field):
     * thay hoa cũ ở ngoài sân nhà bằng hoa ký ức quay đúng hướng.
     * @return số chunk đã trồng lại
     */
    public static int replant(ServerWorld world, int centerChunkX, int centerChunkZ, int radius) {
        int ground = NamMemoryLayout.GROUND_Y;
        int count = 0;
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                world.getChunk(cx, cz);
                int startX = cx << 4;
                int startZ = cz << 4;
                BlockPos.Mutable pos = new BlockPos.Mutable();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int x = startX + dx;
                        int z = startZ + dz;
                        if (NamMemoryLayout.isClearing(x, z) || NamMemoryLayout.isPath(x, z)) continue;
                        world.setBlockState(pos.set(x, ground + 2, z), Blocks.AIR.getDefaultState(), 2 | 16);
                        world.setBlockState(pos.set(x, ground + 1, z), Blocks.AIR.getDefaultState(), 2 | 16);
                    }
                }
                plantChunkOutsideClearing(world, startX, startZ);
                count++;
            }
        }
        return count;
    }

    private static void plantChunkOutsideClearing(ServerWorld world, int startX, int startZ) {
        int ground = NamMemoryLayout.GROUND_Y;
        BlockState sunflower = SamsaraBlocks.MEMORY_SUNFLOWER.getDefaultState();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                if (NamMemoryLayout.isClearing(x, z)) continue;
                if (NamMemoryLayout.isPath(x, z)) {
                    world.setBlockState(pos.set(x, ground, z), Blocks.DIRT_PATH.getDefaultState(), 2);
                    continue;
                }
                TallPlantBlock.placeAt(world, sunflower.with(MemorySunflowerBlock.FACING, facingAt(x, z)),
                        pos.set(x, ground + 1, z), 2);
            }
        }
    }
}
