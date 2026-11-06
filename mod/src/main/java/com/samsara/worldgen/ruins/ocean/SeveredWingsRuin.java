package com.samsara.worldgen.ruins.ocean;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * 3. Spire of Severed Wings (Tháp Đôi Cánh Đứt).
 * Colossal petrified skeletal angelic/draconic wings jutting out from the black mirror sea,
 * sweeping 55 blocks into the sky and framing the northern black hole.
 */
public class SeveredWingsRuin {

    public static boolean generate(ServerWorld world, int centerX, int surfaceY, int centerZ) {
        int waterLevel = Math.max(62, surfaceY);
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        BlockState deepslate = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState basalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 2 wings: left (-X) and right (+X)
        for (int side : new int[]{-1, 1}) {
            int spineLength = 48;

            // Main curved primary wing bone
            for (int i = 0; i <= spineLength; i++) {
                float t = (float) i / (float) spineLength;
                // Arching curve: outwards, then up, then tapering inwards at tip
                int wx = centerX + side * (int) (6 + t * 24.0f - t * t * 8.0f);
                int wy = waterLevel + (int) (t * 52.0f);
                int wz = centerZ + (int) (Math.sin(t * Math.PI) * 12.0f);

                int thickness = (int) Math.max(1, (1.0f - t * 0.7f) * 3.5f);

                for (int dx = -thickness; dx <= thickness; dx++) {
                    for (int dy = -thickness; dy <= thickness; dy++) {
                        for (int dz = -thickness; dz <= thickness; dz++) {
                            if (dx * dx + dy * dy + dz * dz <= thickness * thickness) {
                                mutable.set(wx + dx, wy + dy, wz + dz);
                                BlockState mat = ((i + dx) % 4 == 0) ? deepslate : bone;
                                world.setBlockState(mutable, mat, 2);
                            }
                        }
                    }
                }

                // 4 secondary feather ribs branching backward from the wing spine
                if (i % 8 == 0 && i > 8 && i < 44) {
                    int ribLength = (int) (18.0f * (1.0f - t * 0.5f));
                    for (int r = 0; r <= ribLength; r++) {
                        float rp = (float) r / (float) ribLength;
                        int rx = wx + side * (int) (r * 0.6f);
                        int ry = wy - (int) (rp * 14.0f);
                        int rz = wz - (int) (r * 1.2f);

                        mutable.set(rx, ry, rz);
                        world.setBlockState(mutable, (r == ribLength) ? crying : bone, 2);
                        mutable.set(rx, ry + 1, rz);
                        world.setBlockState(mutable, bone, 2);
                    }
                }
            }
        }

        // Submerged spine socket and anchor rocks
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                mutable.set(centerX + dx, waterLevel - 2, centerZ + dz);
                world.setBlockState(mutable, basalt, 2);
                mutable.set(centerX + dx, waterLevel - 1, centerZ + dz);
                world.setBlockState(mutable, ((dx + dz) % 3 == 0) ? crying : deepslate, 2);
            }
        }

        return true;
    }
}
