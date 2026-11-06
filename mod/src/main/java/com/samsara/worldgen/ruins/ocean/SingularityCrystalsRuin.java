package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 6. Singularity Crystal Spires (Cụm Pha Lê Dị Thường).
 * A cluster of towering crystal needles and geodes piercing up from the abyss through the mirror sea.
 * Formed of Amethyst, Budding Amethyst, Tinted Glass, and Crying Obsidian.
 */
public class SingularityCrystalsRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState amethyst = Blocks.AMETHYST_BLOCK.getDefaultState();
        BlockState budding = Blocks.BUDDING_AMETHYST.getDefaultState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 7 distinct crystal spires with different offsets, heights, and thicknesses
        int[][] spires = {
            {0, 0, 36, 4},      // Central spire: height 36, radius 4
            {-6, 5, 26, 3},     // North-west
            {7, 4, 28, 3},      // North-east
            {-5, -6, 22, 2},    // South-west
            {6, -7, 24, 2},     // South-east
            {0, 10, 18, 2},     // Far North
            {-9, -1, 19, 2}     // Far West
        };

        for (int[] spire : spires) {
            int sx = centerX + spire[0];
            int sz = centerZ + spire[1];
            int height = spire[2];
            int baseR = spire[3];

            for (int y = -6; y <= height; y++) {
                int worldY = waterLevel + y;
                // Radius tapers as height increases
                float progress = Math.max(0.0f, (float) y / (float) height);
                int r = Math.max(0, Math.round(baseR * (1.0f - progress * 0.85f)));

                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dz * dz <= r * r) {
                            mutable.set(sx + dx, worldY, sz + dz);
                            BlockState mat;
                            if (dx == 0 && dz == 0 && y % 8 == 0) {
                                mat = seaLantern; // Glowing core
                            } else if ((dx + dz + y) % 5 == 0) {
                                mat = crying;
                            } else if (progress > 0.6f && (dx * dx + dz * dz == r * r)) {
                                mat = tintedGlass;
                            } else if ((y + dx) % 4 == 0) {
                                mat = budding;
                            } else {
                                mat = amethyst;
                            }
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }

            // Crystal tip
            mutable.set(sx, waterLevel + height + 1, sz);
            world.setBlockState(mutable, crying, 2);
            mutable.set(sx, waterLevel + height + 2, sz);
            world.setBlockState(mutable, tintedGlass, 2);
        }

        return true;
    }
}
