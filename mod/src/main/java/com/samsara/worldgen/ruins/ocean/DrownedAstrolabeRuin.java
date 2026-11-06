package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 1. Drowned Celestial Astrolabe (Vòng Hoàng Đạo Thiên Thể Chìm).
 * Three colossal concentric astronomical gyroscope rings tilted 35 degrees, emerging from the black mirror ocean.
 * Center focal point aligns directly with Gargantua.
 */
public class DrownedAstrolabeRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState gilded = Blocks.GILDED_BLACKSTONE.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 3 concentric rings: R1=24, R2=17, R3=10
        int[] radii = new int[]{24, 17, 10};
        float tiltAngle = 0.61f; // ~35 degrees tilt along X axis

        for (int rIdx = 0; rIdx < radii.length; rIdx++) {
            int radius = radii[rIdx];
            float ringPhase = rIdx * 0.45f;

            for (int deg = 0; deg < 360; deg += 2) {
                double rad = Math.toRadians(deg);
                double localX = radius * Math.cos(rad);
                double unrotatedY = radius * Math.sin(rad);

                // Rotate around X axis
                double localY = unrotatedY * Math.cos(tiltAngle + ringPhase);
                double localZ = unrotatedY * Math.sin(tiltAngle + ringPhase);

                int bx = centerX + (int) Math.round(localX);
                int by = waterLevel + (int) Math.round(localY) + 8;
                int bz = centerZ + (int) Math.round(localZ);

                if (by > 10 && by < 240) {
                    mutable.set(bx, by, bz);
                    BlockState ringMat = (deg % 30 == 0) ? seaLantern : ((deg % 10 == 0) ? crying : ((deg % 4 == 0) ? gilded : blackstone));
                    world.setBlockState(mutable, ringMat, 2);

                    // Add thickness
                    mutable.set(bx, by + 1, bz);
                    world.setBlockState(mutable, blackstone, 2);
                }
            }
        }

        // Central floating singularity sphere / focal core
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= 9) {
                        mutable.set(centerX + dx, waterLevel + 12 + dy, centerZ + dz);
                        BlockState coreMat = (dx * dx + dy * dy + dz * dz <= 3) ? crying : blackstone;
                        world.setBlockState(mutable, coreMat, 2);
                    }
                }
            }
        }

        // Submerged foundation reef
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                if (dx * dx + dz * dz <= 196) {
                    mutable.set(centerX + dx, waterLevel - 4, centerZ + dz);
                    world.setBlockState(mutable, Blocks.SMOOTH_BASALT.getDefaultState(), 2);
                }
            }
        }

        return true;
    }
}
