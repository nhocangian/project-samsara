package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 9. Eternity Void Sundial (Đồng Hồ Vĩnh Cửu Gargantua).
 * A massive 32-block diameter stone dial floating on the mirror ocean.
 * Inscribed with crying obsidian astrological glyphs and a colossal tilted gnomon
 * angled at 33 degrees pointing towards the Gargantua singularity.
 */
public class EternitySundialRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int dialRadius = 16;

        // 1. Dial Face Disk (Y = waterLevel to waterLevel + 1)
        for (int dx = -dialRadius; dx <= dialRadius; dx++) {
            for (int dz = -dialRadius; dz <= dialRadius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq <= dialRadius * dialRadius) {
                    double dist = Math.sqrt(distSq);
                    // Base foundation
                    mutable.set(centerX + dx, waterLevel - 1, centerZ + dz);
                    world.setBlockState(mutable, bricks, 2);

                    // Upper decorative face
                    mutable.set(centerX + dx, waterLevel, centerZ + dz);
                    if (dist >= dialRadius - 1.5) {
                        // Outer rim
                        world.setBlockState(mutable, chiseled, 2);
                    } else if (Math.abs(dist - 10.0) < 1.0) {
                        // Concentric celestial ring
                        world.setBlockState(mutable, crying, 2);
                    } else if (Math.abs(dist - 5.0) < 1.0) {
                        // Inner ring
                        world.setBlockState(mutable, gilded, 2);
                    } else {
                        world.setBlockState(mutable, bricks, 2);
                    }
                }
            }
        }

        // 2. 12 Hour Markers on outer rim
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI / 12) * i;
            int hx = centerX + (int) Math.round((dialRadius - 2) * Math.cos(angle));
            int hz = centerZ + (int) Math.round((dialRadius - 2) * Math.sin(angle));
            mutable.set(hx, waterLevel + 1, hz);
            world.setBlockState(mutable, (i % 3 == 0) ? seaLantern : crying, 2);
            if (i % 3 == 0) {
                mutable.set(hx, waterLevel + 2, hz);
                world.setBlockState(mutable, chiseled, 2);
            }
        }

        // 3. Colossal Tilted Gnomon (Stylus / Pointer)
        // Slanted from center along -Z axis towards the northern horizon
        int gnomonLength = 26;
        for (int step = 0; step < gnomonLength; step++) {
            float t = (float) step / (float) gnomonLength;
            int gz = centerZ - step;
            int maxHeight = (int) (step * 0.95f); // 43 degree angle slope

            for (int gy = 1; gy <= maxHeight; gy++) {
                // Width tapers from 3 blocks at base to 1 at top
                int width = (step < 10) ? 1 : 0;
                for (int gx = -width; gx <= width; gx++) {
                    mutable.set(centerX + gx, waterLevel + gy, gz);
                    BlockState mat = (gy == maxHeight) ? crying : (step % 5 == 0) ? gilded : bricks;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // Gnomon Tip Beacon
        int tipZ = centerZ - (gnomonLength - 1);
        int tipY = waterLevel + (int) ((gnomonLength - 1) * 0.95f);
        mutable.set(centerX, tipY + 1, tipZ);
        world.setBlockState(mutable, seaLantern, 2);
        mutable.set(centerX, tipY + 2, tipZ);
        world.setBlockState(mutable, crying, 2);

        return true;
    }
}
