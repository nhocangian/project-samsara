package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 7. Sunken Colossus Torso (Thân Trên Cự Thần Chìm).
 * Massive tilted torso, spine, and ribcage of a primordial titan jutting out from the black sea.
 * Built of bone blocks, polished basalt, and crying obsidian, with a weeping soul core in the chest.
 */
public class SunkenColossusTorsoRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        BlockState basalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState soulSoil = Blocks.SOUL_SOIL.getDefaultState();
        BlockState soulFire = Blocks.SOUL_FIRE.getDefaultState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. Tilted Spine (slanted along Z axis, Y goes from waterLevel - 6 to waterLevel + 22)
        int spineLength = 28;
        for (int i = 0; i < spineLength; i++) {
            int y = waterLevel - 6 + i;
            int z = centerZ - 10 + (int) Math.round(i * 0.7);
            // Thick spine column (3x3)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    mutable.set(centerX + dx, y, z + dz);
                    world.setBlockState(mutable, (i % 4 == 0) ? crying : bone, 2);
                }
            }
        }

        // 2. Colossal Rib Arcs (5 pairs of massive ribs reaching out into the water)
        int[] ribSpineIndices = {6, 11, 16, 21, 25};
        int[] ribSpans = {14, 16, 17, 15, 12};

        for (int r = 0; r < ribSpineIndices.length; r++) {
            int spineIdx = ribSpineIndices[r];
            int rootY = waterLevel - 6 + spineIdx;
            int rootZ = centerZ - 10 + (int) Math.round(spineIdx * 0.7);
            int span = ribSpans[r];

            for (int side : new int[]{-1, 1}) {
                for (int step = 0; step <= span; step++) {
                    float t = (float) step / (float) span;
                    int rx = centerX + side * (int) (step * 1.1f);
                    // Rib curves upward then drops forward
                    int ry = rootY + (int) (Math.sin(t * Math.PI) * 9.0) - (int) (t * 3.0);
                    int rz = rootZ + (int) (Math.sin(t * Math.PI * 0.5) * 5.0);

                    // 2x2 rib cross-section
                    for (int offY = 0; offY <= 1; offY++) {
                        for (int offZ = 0; offZ <= 1; offZ++) {
                            mutable.set(rx, ry + offY, rz + offZ);
                            world.setBlockState(mutable, ((step + r) % 5 == 0) ? crying : bone, 2);
                        }
                    }
                }
            }
        }

        // 3. Primordial Titan Sternum & Soul Heart Cavity (in the center of chest)
        int heartY = waterLevel + 6;
        int heartZ = centerZ - 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    mutable.set(centerX + dx, heartY + dy, heartZ + dz);
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == -1 || dy == 2) {
                        world.setBlockState(mutable, crying, 2);
                    } else if (dy == 0) {
                        world.setBlockState(mutable, soulSoil, 2);
                    } else if (dy == 1) {
                        world.setBlockState(mutable, soulFire, 2);
                    }
                }
            }
        }

        // 4. Broken Collarbone / Shoulder Scaffolding
        int shoulderY = waterLevel + 18;
        int shoulderZ = centerZ + 6;
        for (int side : new int[]{-1, 1}) {
            for (int dx = 2; dx <= 12; dx++) {
                int px = centerX + side * dx;
                int py = shoulderY - (dx / 3);
                for (int ty = 0; ty <= 1; ty++) {
                    for (int tz = 0; tz <= 1; tz++) {
                        mutable.set(px, py + ty, shoulderZ + tz);
                        world.setBlockState(mutable, (dx == 12) ? bricks : basalt, 2);
                    }
                }
            }
        }

        return true;
    }
}
