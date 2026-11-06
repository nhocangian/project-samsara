package com.samsara.worldgen.megalith;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The Fossilized Hand of the Behemoth (Bàn Tay Cự Thú Bò Xé Mặt Đất - Tham chiếu Ảnh 2 & 4).
 * A gargantuan arm and hand bursting from the earth:
 * - Massive forearm angling 45 degrees up into the clouds (height 190 blocks).
 * - Arched palm forming a colossal natural dome above the terrain.
 * - 4 massive articulated fingers digging into the bedrock like mountain pillars.
 * - Hanging tendrils and black sinews dangling from the knuckles.
 */
public class FossilizedHandFeature {

    public static boolean generate(ServerWorld world, int centerX, int centerZ) {
        int baseY = 62;

        BlockState basalt = Blocks.SMOOTH_BASALT.getDefaultState();
        BlockState mudBricks = Blocks.MUD_BRICKS.getDefaultState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState oxidizedCopper = Blocks.OXIDIZED_COPPER.getDefaultState();

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 1. CỔ TAY VÀ CẲNG TAY VƯƠN CAO LÊN TẦNG TRỜI (Forearm Monolith: Y=baseY to Y=235)
        // Vươn nghiêng 45 độ theo trục Z
        int armLength = 160;
        int armRadius = 14;

        for (int step = 0; step <= armLength; step++) {
            float p = (float) step / (float) armLength;
            int y = baseY + (int) (p * 155.0f);
            int z = centerZ - 60 - (int) (p * 70.0f);
            int curR = (int) (armRadius - p * 3.0f);

            for (int dx = -curR; dx <= curR; dx++) {
                for (int dy = -curR; dy <= curR; dy++) {
                    if (dx * dx + dy * dy <= curR * curR) {
                        mutable.set(centerX + dx, y + dy, z);
                        BlockState mat = ((dx + dy + step) % 5 == 0) ? mudBricks : basalt;
                        if (dy == curR && p < 0.6f) mat = moss; // Rêu phủ trên lưng cánh tay
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        // 2. KHỐI LÒNG BÀN TAY VÀ KHỚP ĐỐT (Knuckle / Palm Dome: Y=125 to Y=165)
        int palmY = baseY + 75;
        int palmZ = centerZ - 30;
        int palmWidth = 48;
        int palmLength = 40;
        int palmThickness = 16;

        for (int px = -palmWidth / 2; px <= palmWidth / 2; px++) {
            for (int pz = -palmLength / 2; pz <= palmLength / 2; pz++) {
                float nx = (float) px / (palmWidth / 2.0f);
                float nz = (float) pz / (palmLength / 2.0f);
                if (nx * nx + nz * nz <= 1.0f) {
                    for (int py = 0; py <= palmThickness; py++) {
                        mutable.set(centerX + px, palmY + py, palmZ + pz);
                        BlockState mat = ((px + py) % 4 == 0) ? deepslateTiles : basalt;
                        if (py == palmThickness && (px + pz) % 3 == 0) mat = moss;
                        world.setBlockState(mutable, mat, 2);
                    }
                }
            }
        }

        // 3. BỐN NGÓN TAY CẮM XUỐNG ĐẤT NHƯ 4 TRỤ CỘT NÚI ĐÁ (4 Giant Finger Pillars)
        int[] fingerOffsetsX = new int[]{-36, -14, 12, 34};
        int[] fingerReachZ = new int[]{25, 45, 48, 30};

        for (int i = 0; i < 4; i++) {
            int startFx = centerX + fingerOffsetsX[i];
            int startFy = palmY + 6;
            int startFz = palmZ + palmLength / 2;

            int targetFx = startFx + (fingerOffsetsX[i] / 4);
            int targetFz = startFz + fingerReachZ[i];
            int targetFy = baseY;

            // Đường cong parabol của ngón tay khum xuống mặt đất
            int fingerSteps = 60;
            int fingerRadius = 7 - (i == 0 || i == 3 ? 1 : 0);

            for (int s = 0; s <= fingerSteps; s++) {
                float fp = (float) s / (float) fingerSteps;
                int fx = (int) (startFx + (targetFx - startFx) * fp);
                int fz = (int) (startFz + (targetFz - startFz) * fp);
                // Đốt ngón tay nhô cao lên ở giữa rồi cắm xuống
                int archBoost = (int) (Math.sin(fp * Math.PI) * 18.0f);
                int fy = (int) (startFy + (targetFy - startFy) * fp + archBoost);

                for (int dx = -fingerRadius; dx <= fingerRadius; dx++) {
                    for (int dz = -fingerRadius; dz <= fingerRadius; dz++) {
                        if (dx * dx + dz * dz <= fingerRadius * fingerRadius) {
                            mutable.set(fx + dx, fy, fz + dz);
                            BlockState mat = ((dx + s) % 6 == 0) ? mudBricks : deepslateTiles;
                            if (fp > 0.85f) mat = cryingObsidian; // Đầu ngón tay cắm xuống đất rỉ năng lượng
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        // 4. CÁC SỢI CÁP / DÂY XÍCH SINH HỌC RỦ XUỐNG TỪ CỔ TAY (Hanging Tendrils - Tham chiếu Ảnh 2)
        // Những sợi dây dài lòng thòng đung đưa trong sương mù
        int[] tendrilDxs = new int[]{-16, -6, 8, 18, 0};
        int[] tendrilDzs = new int[]{-65, -80, -75, -60, -90};

        for (int t = 0; t < tendrilDxs.length; t++) {
            int tx = centerX + tendrilDxs[t];
            int tz = centerZ + tendrilDzs[t];
            int topTy = baseY + 115 - t * 8;
            int len = 50 + t * 9;

            for (int drop = 0; drop <= len; drop++) {
                int ty = topTy - drop;
                // Độ cong nhẹ theo gió
                int curveX = (int) (Math.sin((float) drop * 0.15f) * 3.0f);
                int curveZ = (int) (Math.cos((float) drop * 0.12f) * 3.0f);

                mutable.set(tx + curveX, ty, tz + curveZ);
                world.setBlockState(mutable, ((drop % 3 == 0) ? cryingObsidian : basalt), 2);
            }
        }

        return true;
    }
}
