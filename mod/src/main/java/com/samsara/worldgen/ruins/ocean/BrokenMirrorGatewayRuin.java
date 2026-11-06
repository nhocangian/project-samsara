package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 10. Broken Mirror Gateway (Cổng Gương Vỡ Không Gian).
 * A colossal fractured portal frame rising 38 blocks above the ocean.
 * Broken into floating shards and jagged monoliths by black hole gravitational shear,
 * dripping with crying obsidian tears and sea lantern conduits.
 */
public class BrokenMirrorGatewayRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int frameWidth = 14;  // Half width = 14 (total width 28)
        int frameHeight = 36; // Height above water

        // 1. Two giant pillar bases rooted deep in the seabed
        for (int side : new int[]{-1, 1}) {
            int px = centerX + side * frameWidth;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int y = waterLevel - 8; y <= waterLevel + 1; y++) {
                        mutable.set(px + dx, y, centerZ + dz);
                        world.setBlockState(mutable, ((dx + dz) % 2 == 0) ? bricks : chiseled, 2);
                    }
                }
            }
        }

        // 2. Left Pillar (Mostly intact, reaches Y = waterLevel + 32)
        int leftX = centerX - frameWidth;
        for (int y = 2; y <= 32; y++) {
            // Slight tilt and fractal cracks
            int offZ = (y > 20) ? (y - 20) / 4 : 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(leftX + dx, waterLevel + y, centerZ + dz + offZ);
                    BlockState mat = (y % 5 == 0) ? crying : (y % 8 == 0) ? gilded : bricks;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 3. Right Pillar (Fractured mid-way at Y = waterLevel + 18, top chunk floating detached)
        int rightX = centerX + frameWidth;
        // Lower trunk
        for (int y = 2; y <= 18; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(rightX + dx, waterLevel + y, centerZ + dz);
                    BlockState mat = (y == 18) ? crying : (y % 4 == 0) ? gilded : bricks;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }
        // Floating sheared upper segment (displaced 4 blocks outward and twisted)
        for (int y = 23; y <= 35; y++) {
            int offX = 3 + (y - 23) / 4;
            int offZ = (y - 23) / 3;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(rightX + offX + dx, waterLevel + y, centerZ + offZ + dz);
                    BlockState mat = (y % 3 == 0) ? crying : bricks;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 4. Shattered Top Arch (Arching across top with missing center segment)
        int archTopY = waterLevel + frameHeight;
        for (int x = -frameWidth; x <= frameWidth; x++) {
            // Gap in the center representing shattered bridge
            if (x >= -2 && x <= 4) {
                // Suspended floating debris in the gap
                if (x == 1) {
                    mutable.set(centerX + x, archTopY + 2, centerZ);
                    world.setBlockState(mutable, seaLantern, 2);
                    mutable.set(centerX + x, archTopY + 1, centerZ);
                    world.setBlockState(mutable, crying, 2);
                }
                continue;
            }

            float t = (float) x / (float) frameWidth;
            int archY = archTopY - (int) (t * t * 6.0f);

            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    mutable.set(centerX + x, archY + dy, centerZ + dz);
                    BlockState mat = (dy == 2) ? crying : (x % 3 == 0) ? gilded : bricks;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 5. Floating Shards falling into the ocean below the portal
        int[][] shards = {
            {-6, 12, 2, 2},
            {4, 8, -3, 2},
            {-2, 4, 1, 1},
            {8, 3, 4, 2},
            {0, 2, -2, 1}
        };
        for (int[] shard : shards) {
            int sx = centerX + shard[0];
            int sy = waterLevel + shard[1];
            int sz = centerZ + shard[2];
            int size = shard[3];
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    for (int dz = 0; dz < size; dz++) {
                        mutable.set(sx + dx, sy + dy, sz + dz);
                        world.setBlockState(mutable, ((dx + dy) % 2 == 0) ? crying : bricks, 2);
                    }
                }
            }
        }

        return true;
    }
}
