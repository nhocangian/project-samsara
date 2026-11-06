package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 2. Weeping Obelisk of Sins (Trụ Bia Khóc Tội / Cột Bia Nghiệp Báo).
 * A 42-block tall blackstone obelisk rising from a stepped sunken dais,
 * weeping crying obsidian luminescence and purple tears into the dark sea.
 */
public class WeepingObeliskRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState basalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState soulLantern = Blocks.SOUL_LANTERN.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. Stepped sunken dais emerging from water
        for (int step = 0; step < 4; step++) {
            int r = 16 - step * 3;
            int y = waterLevel - 2 + step;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) <= r && Math.abs(dz) <= r) {
                        mutable.set(centerX + dx, y, centerZ + dz);
                        world.setBlockState(mutable, ((dx + dz) % 3 == 0) ? chiseled : bricks, 2);
                    }
                }
            }
        }

        // 2. Obelisk shaft (height 42 blocks, tapering from 6x6 at base to 2x2 at top)
        int obeliskHeight = 42;
        for (int h = 0; h < obeliskHeight; h++) {
            int y = waterLevel + 2 + h;
            float progress = (float) h / (float) obeliskHeight;
            int halfWidth = (int) Math.round(3.0f * (1.0f - progress * 0.7f));
            halfWidth = Math.max(1, halfWidth);

            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    mutable.set(centerX + dx, y, centerZ + dz);

                    // Carve crying obsidian veins down the 4 faces
                    boolean isFaceChannel = (Math.abs(dx) == halfWidth && dz == 0) || (Math.abs(dz) == halfWidth && dx == 0);
                    if (isFaceChannel && (h % 3 != 0)) {
                        world.setBlockState(mutable, crying, 2);
                    } else if (Math.abs(dx) == halfWidth || Math.abs(dz) == halfWidth) {
                        world.setBlockState(mutable, (h % 5 == 0) ? chiseled : blackstone, 2);
                    } else {
                        world.setBlockState(mutable, basalt, 2);
                    }
                }
            }
        }

        // 3. Pyramidion cap (pointed pyramid tip)
        int tipY = waterLevel + 2 + obeliskHeight;
        mutable.set(centerX, tipY, centerZ);
        world.setBlockState(mutable, crying, 2);
        mutable.set(centerX, tipY + 1, centerZ);
        world.setBlockState(mutable, crying, 2);

        // 4. Soul lanterns at 4 corners of the dais
        for (int sx : new int[]{-7, 7}) {
            for (int sz : new int[]{-7, 7}) {
                mutable.set(centerX + sx, waterLevel + 2, centerZ + sz);
                world.setBlockState(mutable, chiseled, 2);
                mutable.set(centerX + sx, waterLevel + 3, centerZ + sz);
                world.setBlockState(mutable, soulLantern, 2);
            }
        }

        return true;
    }
}
