package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 4. Submerged Ouroboros Temple (Đền Thờ Vòng Rắn Ouroboros Chìm).
 * Circular flooded temple colonnade with 16 monolithic pillars rising from shallow water,
 * surrounding a central sunken altar and eternal soul flame.
 */
public class SubmergedTempleRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState soulSoil = Blocks.SOUL_SOIL.getDefaultState();
        BlockState soulFire = Blocks.SOUL_FIRE.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int templeRadius = 18;

        // 1. Submerged circular floor platform (Y = waterLevel - 2)
        for (int dx = -templeRadius - 4; dx <= templeRadius + 4; dx++) {
            for (int dz = -templeRadius - 4; dz <= templeRadius + 4; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= (templeRadius + 4) * (templeRadius + 4)) {
                    mutable.set(centerX + dx, waterLevel - 2, centerZ + dz);
                    world.setBlockState(mutable, ((dx + dz) % 5 == 0) ? crying : bricks, 2);
                }
            }
        }

        // 2. 16 Colonnade Pillars along the perimeter
        int numPillars = 16;
        for (int i = 0; i < numPillars; i++) {
            double angle = (2 * Math.PI / numPillars) * i;
            int px = centerX + (int) Math.round(templeRadius * Math.cos(angle));
            int pz = centerZ + (int) Math.round(templeRadius * Math.sin(angle));

            int pillarHeight = 14 + (i % 3) * 2; // Some slightly weathered/broken
            for (int h = -1; h <= pillarHeight; h++) {
                for (int cdx = -1; cdx <= 1; cdx++) {
                    for (int cdz = -1; cdz <= 1; cdz++) {
                        mutable.set(px + cdx, waterLevel + h, pz + cdz);
                        BlockState pMat = (h == -1 || h == pillarHeight) ? chiseled : bricks;
                        if ((cdx != 0 && cdz != 0) && h > 0 && h < pillarHeight) {
                            // Rounded column cross
                            continue;
                        }
                        world.setBlockState(mutable, pMat, 2);
                    }
                }
            }

            // Architrave beam connecting to next pillar (for every other pillar)
            if (i % 2 == 0) {
                mutable.set(px, waterLevel + pillarHeight + 1, pz);
                world.setBlockState(mutable, crying, 2);
            }
        }

        // 3. Central Sunken Altar with Soul Fire
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) {
                    mutable.set(centerX + dx, waterLevel - 1, centerZ + dz);
                    world.setBlockState(mutable, chiseled, 2);
                }
            }
        }

        // Central brazier
        mutable.set(centerX, waterLevel, centerZ);
        world.setBlockState(mutable, soulSoil, 2);
        mutable.set(centerX, waterLevel + 1, centerZ);
        world.setBlockState(mutable, soulFire, 2);

        // 4 crying obsidian corner pillars around the altar
        for (int ax : new int[]{-3, 3}) {
            for (int az : new int[]{-3, 3}) {
                for (int h = 0; h <= 3; h++) {
                    mutable.set(centerX + ax, waterLevel + h, centerZ + az);
                    world.setBlockState(mutable, crying, 2);
                }
            }
        }

        return true;
    }
}
