package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 5. Precursor Throne (Ngai Vàng Tiền Thân).
 * A colossal stone throne perched on an isolated jagged basalt crag rising from the black mirror ocean.
 * Built of polished blackstone, gilded blackstone, and crying obsidian, flanked by eternal soul braziers.
 */
public class PrecursorThroneRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState basalt = Blocks.BASALT.getDefaultState();
        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState soulSoil = Blocks.SOUL_SOIL.getDefaultState();
        BlockState soulFire = Blocks.SOUL_FIRE.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. Jagged Basalt Islet (radius ~14, rising Y = waterLevel - 2 to waterLevel + 4)
        int isletRadius = 13;
        for (int dx = -isletRadius; dx <= isletRadius; dx++) {
            for (int dz = -isletRadius; dz <= isletRadius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= isletRadius) {
                    int topY = waterLevel + (int) Math.round((1.0 - (dist / isletRadius)) * 4.5);
                    for (int y = waterLevel - 6; y <= topY; y++) {
                        mutable.set(centerX + dx, y, centerZ + dz);
                        world.setBlockState(mutable, ((dx + y) % 3 == 0) ? smoothBasalt : basalt, 2);
                    }
                }
            }
        }

        // Platform base for the throne (centered at Y = waterLevel + 4)
        int baseHeight = waterLevel + 4;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                mutable.set(centerX + dx, baseHeight, centerZ + dz);
                world.setBlockState(mutable, bricks, 2);
            }
        }

        // 2. Colossal Throne Structure
        // Seat base: X: -5..5, Z: -3..4, Y: baseHeight + 1 to baseHeight + 4
        for (int x = -5; x <= 5; x++) {
            for (int z = -3; z <= 4; z++) {
                for (int y = 1; y <= 4; y++) {
                    mutable.set(centerX + x, baseHeight + y, centerZ + z);
                    BlockState mat = (x == -5 || x == 5 || z == -3) ? bricks : gilded;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // Armrests: X = -5 and X = 5, Z: -3..4, Y: baseHeight + 5 to baseHeight + 8
        for (int armX : new int[]{-5, 5}) {
            for (int z = -3; z <= 4; z++) {
                for (int y = 5; y <= 8; y++) {
                    mutable.set(centerX + armX, baseHeight + y, centerZ + z);
                    world.setBlockState(mutable, (y == 8) ? crying : bricks, 2);
                }
            }
            // Armrest front tips
            mutable.set(centerX + armX, baseHeight + 9, centerZ + 4);
            world.setBlockState(mutable, gilded, 2);
        }

        // Backrest: X: -5..5, Z: -3, Y: baseHeight + 5 to baseHeight + 24
        int backrestTop = baseHeight + 22;
        for (int x = -5; x <= 5; x++) {
            int crownBonus = (x == 0) ? 4 : (Math.abs(x) == 1) ? 3 : (Math.abs(x) == 3) ? 1 : 0;
            int colTop = backrestTop + crownBonus;
            for (int y = 5; y <= colTop - baseHeight; y++) {
                mutable.set(centerX + x, baseHeight + y, centerZ - 3);
                BlockState mat;
                if (Math.abs(x) == 0 && (y == 12 || y == 16 || y == 20)) {
                    mat = crying;
                } else if (Math.abs(x) <= 2) {
                    mat = (y % 4 == 0) ? gilded : bricks;
                } else {
                    mat = bricks;
                }
                world.setBlockState(mutable, mat, 2);
            }
            // Top crown tips with crying obsidian
            mutable.set(centerX + x, colTop + 1, centerZ - 3);
            world.setBlockState(mutable, crying, 2);
        }

        // 3. Flanking Eternal Braziers
        for (int side : new int[]{-9, 9}) {
            for (int h = 1; h <= 6; h++) {
                mutable.set(centerX + side, baseHeight + h, centerZ);
                world.setBlockState(mutable, (h == 6) ? crying : bricks, 2);
            }
            mutable.set(centerX + side, baseHeight + 7, centerZ);
            world.setBlockState(mutable, soulSoil, 2);
            mutable.set(centerX + side, baseHeight + 8, centerZ);
            world.setBlockState(mutable, soulFire, 2);
        }

        return true;
    }
}
