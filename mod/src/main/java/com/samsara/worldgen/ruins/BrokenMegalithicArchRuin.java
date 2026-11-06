package com.samsara.worldgen.ruins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Phế Tích Đại Vòm Cổng Cự Thạch Sụp Đổ (True Colossal Broken Megalithic Arch).
 * Quy mô cự đại:
 * - Nhịp vòm rộng 180 blocks (X từ -90 đến +90).
 * - Chiều cao đỉnh vòm vút lên tận Y = 318 (sát trần thế giới).
 * - Trụ đá dày 18 blocks.
 * - Trụ trái vươn cao qua mây rồi gãy vát ở độ cao Y = 285.
 * - Mặt đất ngổn ngang các tảng cự thạch 14x14 blocks tạo thành hang hốc kỳ vĩ.
 */
public class BrokenMegalithicArchRuin {

    public static boolean generate(ServerWorld world, int centerX, int groundY, int centerZ) {
        Random rand = new Random(centerX * 6673L + centerZ * 3389L);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        BlockState smoothBasalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState crackedDeepslate = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();

        int halfSpanX = 110; // Nhịp rộng 220 blocks
        int apexY = 520; // Vươn cao vượt qua Y=320 cũ lên tận Y = 520!
        int depthZ = 44;
        int archThickness = 22;

        // 1. TRỤ TRÁI VƯƠN CAO LÊN ĐẾN Y=460 RỒI GÃY VÁT
        for (int x = -halfSpanX; x <= -halfSpanX + 65; x++) {
            float xRelOuter = (float) Math.abs(x) / (float) halfSpanX;
            int outerY = (int) (apexY - (apexY - groundY) * (xRelOuter * xRelOuter));

            int innerSpanX = halfSpanX - archThickness;
            int innerApexY = apexY - archThickness;
            int innerY = groundY - 1;
            if (Math.abs(x) < innerSpanX) {
                float xRelInner = (float) Math.abs(x) / (float) innerSpanX;
                innerY = (int) (innerApexY - (innerApexY - groundY) * (xRelInner * xRelInner));
            }

            for (int z = -depthZ / 2; z <= depthZ / 2; z++) {
                int zDrop = (int) (Math.pow((float) Math.abs(z) / (depthZ / 2.0f), 2.0) * 8.0);
                int topLimit = Math.min(560, outerY - zDrop);
                int bottomLimit = Math.max(groundY, innerY + 1);

                // Vết gãy nham nhở ở đầu gãy
                if (x > -halfSpanX + 50 && rand.nextFloat() < 0.45f) continue;
                if (topLimit < bottomLimit) continue;

                for (int y = bottomLimit; y <= topLimit; y++) {
                    mutable.set(centerX + x, y, centerZ + z);
                    BlockState mat;
                    if (y > 420 && x > -halfSpanX + 40) {
                        mat = cryingObsidian;
                    } else if (y > 320) {
                        mat = calcite;
                    } else if (y < groundY + 8 && rand.nextFloat() < 0.35f) {
                        mat = moss;
                    } else if ((x + y + z) % 5 == 0) {
                        mat = crackedDeepslate;
                    } else {
                        mat = smoothBasalt;
                    }
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 2. TRỤ PHẢI GÃY CỤT Ở ĐỘ CAO Y=130
        for (int x = halfSpanX - 32; x <= halfSpanX; x++) {
            int stumpTop = groundY + 45 + (int) (Math.sin((x - halfSpanX) * 0.3f) * 16.0f);
            for (int z = -depthZ / 2; z <= depthZ / 2; z++) {
                for (int y = groundY; y <= stumpTop; y++) {
                    if (rand.nextFloat() < 0.05f) continue;
                    mutable.set(centerX + x, y, centerZ + z);
                    BlockState mat = (y >= stumpTop - 2) ? cryingObsidian : crackedDeepslate;
                    if (y < groundY + 6 && rand.nextFloat() < 0.4f) mat = moss;
                    world.setBlockState(mutable, mat, 2);
                }
            }
        }

        // 3. CÁC TẢNG CỰ THẠCH SẬP ĐỔ KHỔNG LỒ RỘNG 14x14 BLOCKS (Colossal Fallen Boulders)
        int[] rubbleX = new int[]{-20, 15, 42, -5, 26};
        int[] rubbleZ = new int[]{8, -10, 16, -18, 6};
        int[] rubbleSizes = new int[]{11, 14, 10, 13, 8};

        for (int i = 0; i < rubbleX.length; i++) {
            int rx = centerX + rubbleX[i];
            int rz = centerZ + rubbleZ[i];
            int size = rubbleSizes[i];

            for (int dx = -size; dx <= size; dx++) {
                for (int dy = 0; dy <= (int) (size * 1.5f); dy++) {
                    for (int dz = -size; dz <= size; dz++) {
                        if (dx * dx + (dy - size * 0.7f) * (dy - size * 0.7f) + dz * dz <= size * size) {
                            if (rand.nextFloat() < 0.08f) continue;
                            int by = groundY + dy;
                            if (by > 318) break;
                            mutable.set(rx + dx, by, rz + dz);
                            BlockState mat = ((dx + dy + dz) % 4 == 0) ? moss : crackedDeepslate;
                            if (dy > size) mat = smoothBasalt;
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
