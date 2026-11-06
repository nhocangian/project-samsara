package com.samsara.worldgen.ruins;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Phế Tích Đại Đầu Lâu & Bàn Tay Cự Thần (True Colossal Fallen Titan Skull & Grasp).
 * Quy mô ngọn núi đá cự đại:
 * - Đầu lâu rộng 80 blocks, dài 95 blocks, cao 75 blocks, cắm nửa vào lòng đất.
 * - Sừng cự thạch vươn cao chọc trời lên tận Y = 270.
 * - Cửa miệng hang động khổng lồ rộng 24 blocks, cao 20 blocks bước vào lòng sọ như đại thánh đường.
 * - Bàn tay xương khổng lồ vươn cao 110 blocks lên tầng mây.
 */
public class FallenTitanSkullRuin {

    public static boolean generate(ServerWorld world, int centerX, int groundY, int centerZ) {
        Random rand = new Random(centerX * 9973L + centerZ * 5449L);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        BlockState calcite = Blocks.CALCITE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
        BlockState deepslate = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        int skullRadX = 38; // Rộng 76 blocks
        int skullRadY = 32; // Cao 64 blocks
        int skullRadZ = 46; // Dài 92 blocks
        int skullCenterY = groundY + 16;

        // 1. DỰNG VỎ HỘP SỌ CỰ ĐẠI (Titanic Hollow Skull Mountain)
        for (int dx = -skullRadX; dx <= skullRadX; dx++) {
            for (int dy = -skullRadY; dy <= skullRadY; dy++) {
                for (int dz = -skullRadZ; dz <= skullRadZ; dz++) {
                    float nx = (float) dx / skullRadX;
                    float ny = (float) dy / skullRadY;
                    float nz = (float) dz / skullRadZ;

                    float distSq = nx * nx + ny * ny + nz * nz;

                    // Vỏ hộp sọ dày 6 blocks
                    if (distSq <= 1.0f && distSq >= 0.70f) {
                        if (rand.nextFloat() < 0.04f) continue; // Phong hóa nứt toác

                        int curX = centerX + dx + (dy / 4);
                        int curY = skullCenterY + dy;
                        int curZ = centerZ + dz - (dy / 4);
                        if (curY > 318 || curY < -60) continue;

                        // CỬA MIỆNG HANG ĐỘNG KHỔNG LỒ (Mouth Cave: Rộng 24, Cao 20)
                        boolean isMouthCave = (dy >= -skullRadY + 2 && dy <= -skullRadY + 20 && dz < -skullRadZ + 16 && Math.abs(dx) <= 12);
                        // HỐC MẮT HỐ SÂU (Eye Sockets: Đường kính 14 blocks)
                        boolean isEyeSocket = (dy >= 2 && dy <= 16 && dz < -skullRadZ + 12 && (Math.abs(dx) >= 12 && Math.abs(dx) <= 24));

                        mutable.set(curX, curY, curZ);

                        if (isMouthCave || isEyeSocket) {
                            if (curY > groundY) world.setBlockState(mutable, air, 2);
                        } else {
                            BlockState mat = ((dx + dy + dz) % 4 == 0) ? diorite : calcite;
                            if (curY < groundY + 5 && rand.nextFloat() < 0.35f) mat = moss;
                            if (isEyeSocket) mat = cryingObsidian;
                            world.setBlockState(mutable, mat, 2);
                        }
                    } else if (distSq < 0.70f) {
                        // Lòng rỗng bên trong
                        int curX = centerX + dx + (dy / 4);
                        int curY = skullCenterY + dy;
                        int curZ = centerZ + dz - (dy / 4);
                        if (curY > groundY && curY <= 318) {
                            mutable.set(curX, curY, curZ);
                            if (!world.isAir(mutable)) world.setBlockState(mutable, air, 2);
                        }
                    }
                }
            }
        }

        // 2. CẶP SỪNG CỰ THẠCH VƯƠN LÊN TẬN Y=270 (Titanic Horn Spires)
        for (int side : new int[]{-1, 1}) {
            int hornBaseX = centerX + side * 28;
            int hornBaseY = skullCenterY + 24;
            int hornBaseZ = centerZ + 12;

            for (int h = 0; h < 65; h++) {
                int hy = hornBaseY + h;
                if (hy > 318) break;
                float hp = (float) h / 65.0f;
                int hx = hornBaseX + (int) (side * hp * 22.0f);
                int hz = hornBaseZ - (int) (hp * 16.0f);
                int hr = (int) (6.0f * (1.0f - hp * 0.75f));

                for (int dx = -hr; dx <= hr; dx++) {
                    for (int dz = -hr; dz <= hr; dz++) {
                        if (dx * dx + dz * dz <= hr * hr) {
                            mutable.set(hx + dx, hy, hz + dz);
                            BlockState mat = (hp > 0.8f) ? cryingObsidian : bone;
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        // 3. BÀN TAY XƯƠNG CỰ ĐẠI VƯƠN CAO 110 BLOCKS (Colossal Grasping Hand 60 blocks away)
        int handX = centerX + 65;
        int handZ = centerZ - 30;

        // Cổ tay khổng lồ (bán kính 8 blocks)
        for (int dy = 0; dy <= 45; dy++) {
            int hy = groundY + dy;
            if (hy > 318) break;
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (dx * dx + dz * dz <= 64) {
                        mutable.set(handX + dx, hy, handZ + dz);
                        world.setBlockState(mutable, calcite, 2);
                    }
                }
            }
        }

        // 5 ngón tay xương khổng lồ vươn lên mây (dài 60 blocks)
        int[] fingerXOffsets = new int[]{-18, -9, 0, 9, 18};
        for (int f = 0; f < 5; f++) {
            int fx = handX + fingerXOffsets[f];
            int fz = handZ + Math.abs(f - 2) * 5;
            int fLen = 55 - Math.abs(f - 2) * 8;

            for (int step = 0; step <= fLen; step++) {
                int fy = groundY + 45 + step;
                if (fy > 318) break;
                float p = (float) step / fLen;
                int curveZ = fz - (int) (Math.sin(p * Math.PI * 0.7f) * 16.0f);

                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx * dx + dz * dz <= 9) {
                            mutable.set(fx + dx, fy, curveZ + dz);
                            BlockState mat = (step > fLen - 6) ? cryingObsidian : bone;
                            world.setBlockState(mutable, mat, 2);
                        }
                    }
                }
            }
        }

        return true;
    }
}
